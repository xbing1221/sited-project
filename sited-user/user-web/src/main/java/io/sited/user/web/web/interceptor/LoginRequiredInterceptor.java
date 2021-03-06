package io.sited.user.web.web.interceptor;

import com.google.common.collect.Sets;
import io.sited.user.api.UserGroupWebService;
import io.sited.user.api.UserWebService;
import io.sited.user.api.group.BatchGetRequest;
import io.sited.user.api.group.UserGroupResponse;
import io.sited.user.api.user.LoginResponse;
import io.sited.user.api.user.TokenLoginRequest;
import io.sited.user.api.user.UserResponse;
import io.sited.user.web.UserWebOptions;
import io.sited.user.web.service.UserInfoImpl;
import io.sited.web.LoginRequired;
import io.sited.web.SessionInfo;
import io.sited.web.SessionManager;
import io.sited.web.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Cookie;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.sited.user.web.service.UserInfoContextProvider.PROPERTY_USER_INFO;
import static io.sited.user.web.service.UserInfoContextProvider.SESSION_USER_ID;

/**
 * @author chi
 */
@LoginRequired
@Priority(Priorities.AUTHENTICATION)
public class LoginRequiredInterceptor implements ContainerRequestFilter {
    private final Logger logger = LoggerFactory.getLogger(LoginRequiredInterceptor.class);

    @Inject
    UserWebService userWebService;
    @Inject
    UserGroupWebService userGroupWebService;
    @Inject
    UserWebOptions userWebOptions;
    @Inject
    SessionManager sessionManager;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Optional<UserInfo> userInfoOptional = userInfo(requestContext);
        if (userInfoOptional.isPresent() && userInfoOptional.get().isAuthenticated()) {
            UserInfo userInfo = userInfoOptional.get();
            requestContext.setProperty(PROPERTY_USER_INFO, userInfo);
            requestContext.setSecurityContext(userInfo);
        } else {
            throw new NotAuthorizedException("required login");
        }
    }

    private Optional<UserInfo> userInfo(ContainerRequestContext requestContext) {
        UserInfo userInfo = (UserInfo) requestContext.getProperty(PROPERTY_USER_INFO);
        if (userInfo != null) {
            return Optional.of(userInfo);
        }
        SessionInfo sessionInfo = sessionManager.get(requestContext);
        Optional<String> userId = sessionInfo.get(SESSION_USER_ID);
        if (userId.isPresent()) {
            userInfo = user(userWebService.get(userId.get()));
            requestContext.setProperty(PROPERTY_USER_INFO, userInfo);
            requestContext.setSecurityContext(userInfo);
            return Optional.of(userInfo);
        }
        if (userWebOptions.autoLoginEnabled) {
            Optional<UserInfoImpl> userInfoOptional = tryAutoLogin(requestContext, sessionInfo);
            if (userInfoOptional.isPresent()) {
                userInfo = userInfoOptional.get();
                return Optional.of(userInfo);
            }
        }
        return Optional.empty();
    }

    private Optional<UserInfoImpl> tryAutoLogin(ContainerRequestContext request, SessionInfo session) {
        Cookie cookie = request.getCookies().get(userWebOptions.autoLoginCookie);
        if (cookie != null) {
            try {
                TokenLoginRequest authenticationRequest = new TokenLoginRequest();
                authenticationRequest.token = cookie.getValue();
                LoginResponse authenticationResponse = userWebService.login(authenticationRequest);
                UserInfoImpl user = user(authenticationResponse.user);
                session.put(SESSION_USER_ID, user.id());
                return Optional.of(user);
            } catch (Throwable e) {
                logger.warn("invalid auto login token cookie, value={}", cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private UserInfoImpl user(UserResponse response) {
        UserInfoImpl user = new UserInfoImpl();
        user.id = response.id;
        user.username = response.username;
        user.nickname = response.nickname;
        user.phone = response.phone;
        user.email = response.email;
        user.imageURL = response.imageURL;
        user.authenticated = true;

        BatchGetRequest request = new BatchGetRequest();
        request.ids = response.userGroupIds;
        List<UserGroupResponse> groups = userGroupWebService.batchGet(request);
        Set<String> roles = Sets.newHashSet();
        for (UserGroupResponse group : groups) {
            roles.addAll(group.roles);
        }
        user.roles = roles;
        return user;
    }
}
