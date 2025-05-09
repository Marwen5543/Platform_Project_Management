export const environment = {
    production: true,
    keycloak: {
      url: 'http://keycloak:8080', // Docker service name
      realm: 'Tunisys',
      clientId: 'demo-rest-api',
      enablePkce: true,
      publicClient: true,
      postLogoutRedirectUri: window.location.origin
    }
  };