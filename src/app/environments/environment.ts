export const environment = {
  production: false,
  keycloak: {
    url: 'http://localhost:8080', // Local Keycloak server
    realm: 'Tunisys',
    clientId: 'angular-app',
    enablePkce: true,
    publicClient: true,
    postLogoutRedirectUri: window.location.origin
  }
};