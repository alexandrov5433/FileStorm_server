# FileStorm
## About
FileStorm is a cloud service for storing and sharing files. This is the FileStorm server. FileStorm is a personal portfolio project.
## Account
In order to use the service the user must create a free account. At the time of writting each account recieves 50 GB of avalilable storage. Non-users (guests) may only download files, through a sharing link, which were made publicly available from users. 
## Hosting and Deployment
GitHub Actions were utilized to build and deploy the server to an EC2 instance at AWS. The Java application runs in a Docker container.
## Files
At the time of writting, the maximum upload limit for one file is 10 GB and the size limit of the multipart request is 11 GB. May be chaged in the future. Empty files can not be uploaded. The uploaded files are stored in a mounted EFS.

When the user marks multiple files and directories and selects the 'Download Selected' option the files are send to the user in the form of a .TAR file. The .TAR is streamed as it is being created, allowing the server to not use extra memory or storage space solely for the creation of the file. An additional benefit is the instantaneous start of the download process, regardless of the size of the .TAR file.
## Sharing
Files my be shared with all other users of the service from the 'Share' option in the dropdown menu of the respective file. A user, reciever of the shared file, may find it in the 'Shared With Me' section of his storage page.
## Favorites
Files my be marked as favorite and so be added to the favorits list in the 'Favorites' secion. This is done through the star-icon which appears when the file is hovered.