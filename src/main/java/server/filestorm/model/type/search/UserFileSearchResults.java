package server.filestorm.model.type.search;

public class UserFileSearchResults {
    public FileSearchResult[] myStorageResults;
    public FileSearchResult[] sharedWithMeResults;

    public UserFileSearchResults() {
        this.myStorageResults = new FileSearchResult[0];
        this.sharedWithMeResults = new FileSearchResult[0];
    }

    public UserFileSearchResults(FileSearchResult[] myStoragResults, FileSearchResult[] sharedWithMResults) {
        this.myStorageResults = myStoragResults;
        this.sharedWithMeResults = sharedWithMResults;
    }

    public void setMyStorageResults(FileSearchResult[] myStorageResults) {
        this.myStorageResults = myStorageResults;
    }

    public FileSearchResult[] getMyStorageResults() {
        return myStorageResults;
    }

    public void setSharedWithMeResults(FileSearchResult[] sharedWithMeResults) {
        this.sharedWithMeResults = sharedWithMeResults;
    }

    public FileSearchResult[] getSharedWithMeResults() {
        return sharedWithMeResults;
    }
}
