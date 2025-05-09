import { environment } from "../environments/environment";

console.log('Loaded environment:', environment); // Debug log

export const keycloakConfig = {
  url: environment.keycloak.url,
  realm: environment.keycloak.realm,
  clientId: environment.keycloak.clientId,
  enablePkce: environment.keycloak.enablePkce,
  publicClient: environment.keycloak.publicClient,
  postLogoutRedirectUri: environment.keycloak.postLogoutRedirectUri
};