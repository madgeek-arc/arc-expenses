package service;

public interface LogInService {
    String getAuthNRedirectUrl(String idpAppURL, String assertionConsumerServiceUrl, String issuerId);
}
