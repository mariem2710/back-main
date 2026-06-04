public class MantisIssueResponse {
    private IssueRef issue;

    public static class IssueRef {
        private Long id;
        public Long getId() { return id; }
    }

    public IssueRef getIssue() { return issue; }

    public Long getIssueId() {
        return issue != null ? issue.getId() : null;
    }
}