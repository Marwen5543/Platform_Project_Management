import Keycloak from 'keycloak-js';

declare module 'keycloak-js' {
    interface KeycloakTokenParsed {
        preferred_username?: string;
        sub?: string;
        email?: string;
    }
}

declare module 'jwt-decode' {
    export default function jwtDecode<T>(token: string): T;
  }