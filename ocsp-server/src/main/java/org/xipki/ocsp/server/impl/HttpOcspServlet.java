/*
 *
 * Copyright (c) 2013 - 2017 Lijun Liao
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation with the addition of the
 * following permission added to Section 15 as permitted in Section 7(a):
 *
 * FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
 * THE AUTHOR LIJUN LIAO. LIJUN LIAO DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
 * OF THIRD PARTY RIGHTS.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the XiPKI software without
 * disclosing the source code of your own applications.
 *
 * For more information, please contact Lijun Liao at this
 * address: lijun.liao@gmail.com
 */

package org.xipki.ocsp.server.impl;

import java.io.EOFException;

import javax.net.ssl.SSLSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.common.util.Base64;
import org.xipki.common.util.LogUtil;
import org.xipki.common.util.ParamUtil;
import org.xipki.http.servlet.AbstractHttpServlet;
import org.xipki.http.servlet.ServletURI;
import org.xipki.http.servlet.SslReverseProxyMode;
import org.xipki.ocsp.server.impl.OcspRespWithCacheInfo.ResponseCacheInfo;
import org.xipki.security.HashAlgoType;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class HttpOcspServlet extends AbstractHttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(HttpOcspServlet.class);

    private static final String CT_REQUEST = "application/ocsp-request";

    private static final String CT_RESPONSE = "application/ocsp-response";

    private OcspServer server;

    public HttpOcspServlet() {
    }

    public void setServer(final OcspServer server) {
        this.server = ParamUtil.requireNonNull("server", server);
    }

    @Override
    public FullHttpResponse service(FullHttpRequest request, ServletURI servletUri,
            SSLSession sslSession, SslReverseProxyMode sslReverseProxyMode) throws Exception {
        if (server == null) {
            String message = "responder in servlet not configured";
            LOG.error(message);
            return createErrorResponse(request.protocolVersion(),
                    HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }

        HttpMethod method = request.method();
        if (HttpMethod.POST.equals(method)) {
            return servicePost(request, servletUri, sslSession, sslReverseProxyMode);
        } else if (HttpMethod.GET.equals(method)) {
            return serviceGet(request, servletUri, sslSession, sslReverseProxyMode);
        } else {
            return createErrorResponse(request.protocolVersion(),
                    HttpResponseStatus.METHOD_NOT_ALLOWED);
        }

    }

    private FullHttpResponse servicePost(FullHttpRequest request, ServletURI servletUri,
            SSLSession sslSession, SslReverseProxyMode sslReverseProxyMode) throws Exception {
        HttpVersion version = request.protocolVersion();

        Responder responder = server.getResponder(servletUri);
        if (responder == null) {
            return createErrorResponse(version, HttpResponseStatus.NOT_FOUND);
        }

        try {
            // accept only "application/ocsp-request" as content type
            String reqContentType = request.headers().get("Content-Type");
            if (!CT_REQUEST.equalsIgnoreCase(reqContentType)) {
                return createErrorResponse(version, HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE);
            }

            int contentLen = request.content().readableBytes();
            // request too long
            if (contentLen > responder.requestOption().maxRequestSize()) {
                return createErrorResponse(version,
                        HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
            }

            OcspRespWithCacheInfo ocspRespWithCacheInfo = server.answer(responder,
                    readContent(request), false);
            if (ocspRespWithCacheInfo == null || ocspRespWithCacheInfo.response() == null) {
                LOG.error("processRequest returned null, this should not happen");
                return createErrorResponse(version, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            }

            byte[] encodedOcspResp = ocspRespWithCacheInfo.response();
            return createOKResponse(version, CT_RESPONSE, encodedOcspResp);
        } catch (Throwable th) {
            if (th instanceof EOFException) {
                LogUtil.warn(LOG, th, "Connection reset by peer");
            } else {
                LOG.error("Throwable thrown, this should not happen!", th);
            }
            return createErrorResponse(version, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        } // end external try
    } // method servicePost

    private FullHttpResponse serviceGet(FullHttpRequest request, ServletURI servletUri,
            SSLSession sslSession, SslReverseProxyMode sslReverseProxyMode) throws Exception {
        HttpVersion version = request.protocolVersion();

        Object[] objs = server.getServletPathAndResponder(servletUri);
        if (objs == null) {
            return createErrorResponse(version, HttpResponseStatus.NOT_FOUND);
        }

        String path = servletUri.path();
        String servletPath = (String) objs[0];
        Responder responder = (Responder) objs[1];

        if (!responder.requestOption().supportsHttpGet()) {
            return createErrorResponse(version, HttpResponseStatus.METHOD_NOT_ALLOWED);
        }

        String b64OcspReq;

        int offset = servletPath.length();
        // GET URI contains the request and must be much longer than 10.
        if (path.length() - offset > 10) {
            if (path.charAt(offset) == '/') {
                offset++;
            }
            b64OcspReq = servletUri.path().substring(offset);
        } else {
            return createErrorResponse(version, HttpResponseStatus.BAD_REQUEST);
        }

        try {
            // RFC2560 A.1.1 specifies that request longer than 255 bytes SHOULD be sent by
            // POST, we support GET for longer requests anyway.
            if (b64OcspReq.length() > responder.requestOption().maxRequestSize()) {
                return createErrorResponse(version, HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
            }

            OcspRespWithCacheInfo ocspRespWithCacheInfo = server.answer(responder,
                    Base64.decode(b64OcspReq), true);
            if (ocspRespWithCacheInfo == null || ocspRespWithCacheInfo.response() == null) {
                return createErrorResponse(version, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            }

            byte[] encodedOcspResp = ocspRespWithCacheInfo.response();

            FullHttpResponse response = createOKResponse(version, CT_RESPONSE, encodedOcspResp);

            ResponseCacheInfo cacheInfo = ocspRespWithCacheInfo.cacheInfo();
            if (cacheInfo != null) {
                encodedOcspResp = ocspRespWithCacheInfo.response();
                long now = System.currentTimeMillis();

                HttpHeaders headers = response.headers();
                // RFC 5019 6.2: Date: The date and time at which the OCSP server generated
                // the HTTP response.
                headers.add("Date", now);
                // RFC 5019 6.2: Last-Modified: date and time at which the OCSP responder
                // last modified the response.
                headers.add("Last-Modified", cacheInfo.thisUpdate());
                // RFC 5019 6.2: Expires: This date and time will be the same as the
                // nextUpdate time-stamp in the OCSP
                // response itself.
                // This is overridden by max-age on HTTP/1.1 compatible components
                if (cacheInfo.nextUpdate() != null) {
                    headers.add("Expires", cacheInfo.nextUpdate());
                }
                // RFC 5019 6.2: This profile RECOMMENDS that the ETag value be the ASCII
                // HEX representation of the SHA1 hash of the OCSPResponse structure.
                headers.add("ETag",
                        new StringBuilder(42).append('\\')
                            .append(HashAlgoType.SHA1.hexHash(encodedOcspResp))
                            .append('\\')
                        .toString());

                // Max age must be in seconds in the cache-control header
                long maxAge;
                if (responder.responseOption().cacheMaxAge() != null) {
                    maxAge = responder.responseOption().cacheMaxAge().longValue();
                } else {
                    maxAge = OcspServer.DFLT_CACHE_MAX_AGE;
                }

                if (cacheInfo.nextUpdate() != null) {
                    maxAge = Math.min(maxAge,
                            (cacheInfo.nextUpdate() - cacheInfo.thisUpdate()) / 1000);
                }

                headers.add("Cache-Control",
                        new StringBuilder(55).append("max-age=").append(maxAge)
                            .append(",public,no-transform,must-revalidate").toString());
            } // end if (ocspRespWithCacheInfo)

            return response;
        } catch (Throwable th) {
            if (th instanceof EOFException) {
                LogUtil.warn(LOG, th, "Connection reset by peer");
            } else {
                LOG.error("Throwable thrown, this should not happen!", th);
            }
            return createErrorResponse(version, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        } // end external try
    } // method serviceGet

}
