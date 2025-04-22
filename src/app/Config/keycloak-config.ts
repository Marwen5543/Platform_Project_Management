// src/app/Config/keycloak-config.ts
export const keycloakConfig = {
  url: 'http://localhost:8080',          
  realm: 'Tunisys',                      
  clientId: 'demo-rest-api',             
  enablePkce: true,                      
  publicClient: true,                    
  postLogoutRedirectUri: window.location.origin 
};