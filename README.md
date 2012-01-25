Very simple synchronization utility for uploading local images to your SmugMug account.

Specify the folder to synchronize within SmugSynch.java and your SmugMug user/password in SmugMug.java.

For every subfolder encountered, SmugSynch will create a folder on SmugMug and synchronize the contents.  Only new images will be uploaded.

*TODO*

* parameterize folder to synchronize and SmugMug user/password as arguments to SmugSynch

*Known Limitations*

* Folders within folders are not synchronized
* Only considers .JPG or .jpeg files
