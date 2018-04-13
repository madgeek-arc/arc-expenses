package service;

public interface UserService {
    String getAuthNRedirectUrl(String idpAppURL, String assertionConsumerServiceUrl, String issuerId);
}
