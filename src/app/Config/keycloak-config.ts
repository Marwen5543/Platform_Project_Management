export const keycloakConfig = {
  url: 'http://localhost:8080',
  realm: 'Tunisys',
  clientId: 'demo-rest-api',
  "enable-pkce": true,
  "public-client": true,
  "post-logout-redirect-uri": "http://localhost:4200/"
};