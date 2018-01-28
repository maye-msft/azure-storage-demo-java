package com.microsoft.csegcr.storage.blob;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import com.microsoft.azure.keyvault.extensions.RsaKey;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.BlobContainerPermissions;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.BlobEncryptionPolicy;
import com.microsoft.azure.storage.blob.BlobRequestOptions;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;

//fix the issue of java.security.InvalidKeyException: Illegal key size or default parameters
//https://stackoverflow.com/questions/24907530/java-security-invalidkeyexception-illegal-key-size-or-default-parameters-in-and
//https://stackoverflow.com/questions/6481627/java-security-illegal-key-size-or-default-parameters/6481658#6481658

public class EncryptClient {
	
	public static void main(String[] args) throws InvalidKeyException, URISyntaxException {
		

		// Setup the cloud storage account.
		CloudStorageAccount account = CloudStorageAccount.parse("DefaultEndpointsProtocol=https;AccountName=mayegrs20180127;AccountKey=zgNMMgF4Y9+oCAPdNnm/eX/4p9G9fh0OiEPhHtBn5nCap2eY4cy5LOH0j8U2Z/U4GLUE/X6Tvzs0Joj84UhFKA==;EndpointSuffix=core.windows.net");

		// Create a blob service client
		CloudBlobClient blobClient = account.createCloudBlobClient();
		
		try {
			// Get a reference to a container
			// The container name must be lower case
			CloudBlobContainer container = blobClient.getContainerReference("myjavacontainer");

			// Create the container if it does not exist
			container.createIfNotExists();
			
			// Make the container public
			// Create a permissions object
			BlobContainerPermissions containerPermissions = new BlobContainerPermissions();

			// Include public access in the permissions object
			containerPermissions.setPublicAccess(BlobContainerPublicAccessType.BLOB);

			// Set the permissions on the container
			container.uploadPermissions(containerPermissions);
			
			// Upload 3 blobs
			// Get a reference to a blob in the container
			CloudBlockBlob blob1 = container.getBlockBlobReference("blobbasicsblob1");
			
			// Upload text to the blob
			blob1.uploadText("Hello, World1");
			
			// Get a reference to a blob in the container
			CloudBlockBlob blob2 = container.getBlockBlobReference("blobbasicsblob2");
			
			// Create the IKey used for encryption.
            final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            final KeyPair wrapKey = keyGen.generateKeyPair();
            RsaKey key = new RsaKey("rsaKey", wrapKey);

            // Create the encryption policy to be used for upload.
            BlobEncryptionPolicy uploadPolicy = new BlobEncryptionPolicy(key,
                    null);

            // Set the encryption policy on the request options.
            BlobRequestOptions uploadOptions = new BlobRequestOptions();
            uploadOptions.setEncryptionPolicy(uploadPolicy);
            
            
            // Upload text to the blob2
            byte[] buffer = (new String("Hello, World2")).getBytes();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(buffer);
            blob2.upload(inputStream, buffer.length, null, uploadOptions, null);
			
			// List the blobs in a container, loop over them and 
			// output the URI of each of them
			for (ListBlobItem blobItem : container.listBlobs()) {
			    System.out.println(blobItem.getUri());
			}
			
			// Download the blob
			// For each item in the container
			for (ListBlobItem blobItem : container.listBlobs()) {
			    // If the item is a blob, not a virtual directory
			    if (blobItem instanceof CloudBlockBlob) {
			        // Download the text
			    	CloudBlockBlob retrievedBlob = (CloudBlockBlob) blobItem;
			    	System.out.println(retrievedBlob.downloadText());
			    }
			}
			
			LocalResolver resolver = new LocalResolver();
            resolver.add(key);
            BlobEncryptionPolicy downloadPolicy = new BlobEncryptionPolicy(
                    null, resolver);

            // Set the decryption policy on the request options.
            BlobRequestOptions downloadOptions = new BlobRequestOptions();
            downloadOptions.setEncryptionPolicy(downloadPolicy);

            System.out.println("Downloading the encrypted blob.");

            // Download and decrypt the encrypted contents from the blob.
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            blob2.download(outputStream, null, downloadOptions, null);
            System.out.println(new String(outputStream.toByteArray()));
			

		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}
		

}

