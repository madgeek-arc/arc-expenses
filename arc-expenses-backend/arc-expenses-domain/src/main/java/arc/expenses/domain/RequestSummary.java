package arc.expenses.domain;

import gr.athenarc.domain.BaseInfo;
import gr.athenarc.domain.Request;

public class RequestSummary {

    private BaseInfo baseInfo;
    private Request request;

    public RequestSummary() { }

    public BaseInfo getBaseInfo() {
        return baseInfo;
    }

    public void setBaseInfo(BaseInfo baseInfo) {
        this.baseInfo = baseInfo;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }
}
