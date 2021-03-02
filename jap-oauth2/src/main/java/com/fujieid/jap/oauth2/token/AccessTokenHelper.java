/*
 * Copyright (c) 2020-2040, 北京符节科技有限公司 (support@fujieid.com & https://www.fujieid.com).
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fujieid.jap.oauth2.token;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.fujieid.jap.core.util.JapUtil;
import com.fujieid.jap.core.exception.JapOauth2Exception;
import com.fujieid.jap.oauth2.*;
import com.fujieid.jap.oauth2.pkce.PkceHelper;
import com.fujieid.jap.oauth2.pkce.PkceParams;
import com.google.common.collect.Maps;
import com.xkcoding.http.HttpUtil;
import com.xkcoding.json.JsonUtil;
import com.xkcoding.json.util.Kv;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Access token helper. Provides a unified access token method {@link AccessTokenHelper#getToken(HttpServletRequest, OAuthConfig)}
 * for different authorization methods
 *
 * @author yadong.zhang (yadong.zhang0415(a)gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 */
public class AccessTokenHelper {

    private AccessTokenHelper() {
    }

    /**
     * get access_token
     *
     * @param request     Current callback request
     * @param oAuthConfig oauth config
     * @return AccessToken
     */
    public static AccessToken getToken(HttpServletRequest request, OAuthConfig oAuthConfig) throws JapOauth2Exception {
        if (null == oAuthConfig) {
            throw new JapOauth2Exception("Oauth2Strategy failed to get AccessToken. OAuthConfig cannot be empty");
        }
        if (oAuthConfig.getResponseType() == Oauth2ResponseType.code) {
            return getAccessTokenOfAuthorizationCodeMode(request, oAuthConfig);
        }
        if (oAuthConfig.getResponseType() == Oauth2ResponseType.token) {
            return getAccessTokenOfImplicitMode(request);
        }
        if (oAuthConfig.getGrantType() == Oauth2GrantType.password) {
            return getAccessTokenOfPasswordMode(request, oAuthConfig);
        }
        if (oAuthConfig.getGrantType() == Oauth2GrantType.client_credentials) {
            return getAccessTokenOfClientMode(request, oAuthConfig);
        }
        throw new JapOauth2Exception("Oauth2Strategy failed to get AccessToken. Missing required parameters");
    }


    /**
     * 4.1.  Authorization Code Grant
     *
     * @param request     current callback request
     * @param oAuthConfig oauth config
     * @return token request url
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1" target="_blank">4.1.  Authorization Code Grant</a>
     */
    private static AccessToken getAccessTokenOfAuthorizationCodeMode(HttpServletRequest request, OAuthConfig oAuthConfig) throws JapOauth2Exception {
        String state = request.getParameter("state");
        Oauth2Util.checkState(state, oAuthConfig.getClientId(), oAuthConfig.isVerifyState());

        String code = request.getParameter("code");
        Map<String, String> params = Maps.newHashMap();
        params.put("grant_type", Oauth2GrantType.authorization_code.name());
        params.put("code", code);
        params.put("client_id", oAuthConfig.getClientId());
        params.put("client_secret", oAuthConfig.getClientSecret());
        if (StrUtil.isNotBlank(oAuthConfig.getCallbackUrl())) {
            params.put("redirect_uri", oAuthConfig.getCallbackUrl());
        }
        // Pkce is only applicable to authorization code mode
        if (Oauth2ResponseType.code == oAuthConfig.getResponseType() && oAuthConfig.isEnablePkce()) {
            params.put(PkceParams.CODE_VERIFIER, PkceHelper.getCacheCodeVerifier(oAuthConfig.getClientId()));
        }

        String tokenResponse = HttpUtil.post(oAuthConfig.getTokenUrl(), params, false);
        Kv tokenInfo = JsonUtil.parseKv(tokenResponse);
        Oauth2Util.checkOauthResponse(tokenResponse, tokenInfo, "Oauth2Strategy failed to get AccessToken.");

        if (!tokenInfo.containsKey("access_token")) {
            throw new JapOauth2Exception("Oauth2Strategy failed to get AccessToken." + tokenResponse);
        }

        return mapToAccessToken(tokenInfo);
    }

    /**
     * 4.2.  Implicit Grant
     *
     * @param request current callback request
     * @return token request url
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.2" target="_blank">4.2.  Implicit Grant</a>
     */
    private static AccessToken getAccessTokenOfImplicitMode(HttpServletRequest request) throws JapOauth2Exception {
        Oauth2Util.checkOauthCallbackRequest(request.getParameter("error"), request.getParameter("error_description"),
            "Oauth2Strategy failed to get AccessToken.");

        if (null == request.getParameter("access_token")) {
            throw new JapOauth2Exception("Oauth2Strategy failed to get AccessToken.");
        }

        return new AccessToken()
            .setAccessToken(request.getParameter("access_token"))
            .setRefreshToken(request.getParameter("refresh_token"))
            .setIdToken(request.getParameter("id_token"))
            .setTokenType(request.getParameter("token_type"))
            .setScope(request.getParameter("scope"))
            .setExpiresIn(JapUtil.toInt(request.getParameter("expires_in")));
    }

    /**
     * 4.3.  Resource Owner Password Credentials Grant
     *
     * @param oAuthConfig oauth config
     * @return token request url
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.3" target="_blank">4.3.  Resource Owner Password Credentials Grant</a>
     */
    private static AccessToken getAccessTokenOfPasswordMode(HttpServletRequest request, OAuthConfig oAuthConfig) throws JapOauth2Exception {
        Map<String, String> params = Maps.newHashMap();
        params.put("grant_type", Oauth2GrantType.password.name());
        params.put("username", oAuthConfig.getUsername());
        params.put("password", oAuthConfig.getPassword());
        params.put("client_id", oAuthConfig.getClientId());
        params.put("client_secret", oAuthConfig.getClientSecret());
        if (ArrayUtil.isNotEmpty(oAuthConfig.getScopes())) {
            params.put("scope", String.join(Oauth2Const.SCOPE_SEPARATOR, oAuthConfig.getScopes()));
        }
        String url = oAuthConfig.getTokenUrl();
        String tokenResponse = HttpUtil.post(url, params, false);
        Kv tokenInfo = JsonUtil.parseKv(tokenResponse);
        Oauth2Util.checkOauthResponse(tokenResponse, tokenInfo, "Oauth2Strategy failed to get AccessToken.");

        if (!tokenInfo.containsKey("access_token")) {
            throw new JapOauth2Exception("Oauth2Strategy failed to get AccessToken." + tokenResponse);
        }
        return mapToAccessToken(tokenInfo);
    }

    /**
     * 4.4.  Client Credentials Grant
     *
     * @param oAuthConfig oauth config
     * @return token request url
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.4" target="_blank">4.4.  Client Credentials Grant</a>
     */
    private static AccessToken getAccessTokenOfClientMode(HttpServletRequest request, OAuthConfig oAuthConfig) throws JapOauth2Exception {
        throw new JapOauth2Exception("Oauth2Strategy failed to get AccessToken. Grant type of client_credentials type is not supported.");
//        Map<String, String> params = Maps.newHashMap();
//        params.put("grant_type", Oauth2GrantType.client_credentials.name());
//        if (ArrayUtil.isNotEmpty(oAuthConfig.getScopes())) {
//            params.put("scope", String.join(Oauth2Const.SCOPE_SEPARATOR, oAuthConfig.getScopes()));
//        }
//        String url = oAuthConfig.getTokenUrl();
//
//        String tokenResponse = HttpUtil.post(url, params, false);
//        Kv tokenInfo = JsonUtil.parseKv(tokenResponse);
//        Oauth2Util.checkOauthResponse(tokenResponse, tokenInfo, "Oauth2Strategy failed to get AccessToken.");
//
//        if (ObjectUtil.isEmpty(request.getParameter("access_token"))) {
//            throw new JapOauth2Exception("Oauth2Strategy failed to get AccessToken.");
//        }
//
//        return mapToAccessToken(tokenInfo);
    }

    private static AccessToken mapToAccessToken(Kv tokenMap) {
        return new AccessToken()
            .setAccessToken(tokenMap.getString("access_token"))
            .setRefreshToken(tokenMap.getString("refresh_token"))
            .setIdToken(tokenMap.getString("id_token"))
            .setTokenType(tokenMap.getString("token_type"))
            .setScope(tokenMap.getString("scope"))
            .setExpiresIn(tokenMap.getInteger("expires_in"));
    }
}
