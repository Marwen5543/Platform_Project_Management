export const environment = {
  production: false,
  keycloak: {
    url: 'http://localhost:8080', // Local Keycloak server
    realm: 'Tunisys',
    clientId: 'demo-rest-api',
    enablePkce: true,
    publicClient: true,
    postLogoutRedirectUri: window.location.origin
  }
};