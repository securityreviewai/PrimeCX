const oktaConfig = {
  issuer: process.env.REACT_APP_OKTA_ISSUER || 'https://{yourOktaDomain}/oauth2/default',
  clientId: process.env.REACT_APP_OKTA_CLIENT_ID || '{yourClientId}',
  redirectUri: window.location.origin + '/login/callback',
  scopes: ['openid', 'profile', 'email'],
  pkce: true,
};

export default oktaConfig;
