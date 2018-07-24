package eu.openanalytics.phaedra.base.fs.s3;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import eu.openanalytics.phaedra.base.fs.BaseFileServer;
import eu.openanalytics.phaedra.base.fs.FileServerConfig;
import eu.openanalytics.phaedra.base.util.io.CachingByteRange.DataFetcher;
import eu.openanalytics.phaedra.base.util.io.CachingSeekableChannel;

public class S3Interface extends BaseFileServer {

	private AmazonS3 s3;
	private String bucketName;
	private boolean enableSSE;
	
	@Override
	public boolean isCompatible(FileServerConfig cfg) {
		String fsPath = cfg.get(FileServerConfig.PATH);
		return fsPath.startsWith("https://");
	}

	@Override
	public void initialize(FileServerConfig cfg) throws IOException {
		String fsPath = cfg.get(FileServerConfig.PATH);
		String userName = cfg.get(FileServerConfig.USERNAME);
		String pw = cfg.getEncrypted(FileServerConfig.PASSWORD);
		
		bucketName = getLocalName(fsPath);
		fsPath = fsPath.substring(0, fsPath.lastIndexOf("/"));
		
		enableSSE = Boolean.valueOf(cfg.get("enable.sse"));
		
		BasicAWSCredentials credentials = new BasicAWSCredentials(userName, pw);
		EndpointConfiguration endpointConfiguration = new EndpointConfiguration(fsPath, null);

		s3 = AmazonS3ClientBuilder.standard()
				.withEndpointConfiguration(endpointConfiguration)
				.enablePathStyleAccess()
				.withCredentials(new AWSStaticCredentialsProvider(credentials)).build();
	}

	@Override
	public void close() throws IOException {
		// Nothing to close.
	}

	@Override
	public long getFreeSpace() throws IOException {
		// There is no such thing as free space on S3.
		return 0;
	}
	
	@Override
	public long getTotalSpace() throws IOException {
		// There is no such thing as total space on S3.
		return 0;
	}
	
	@Override
	public boolean exists(String path) throws IOException {
		return s3.doesObjectExist(bucketName, getKey(path));
	}

	@Override
	public boolean isDirectory(String path) throws IOException {
		return !dir(path).isEmpty();
	}

	@Override
	public long getCreateTime(String path) throws IOException {
		// Note: S3 objects are immutable. Create time == last mod time.
		return getLastModified(path);
	}

	@Override
	public long getLastModified(String path) throws IOException {
		try (S3Object o = s3.getObject(bucketName, getKey(path))) {
			return o.getObjectMetadata().getLastModified().getTime();
		}
	}

	@Override
	public List<String> dir(String path) throws IOException {
		String key = getKey(path);
		if (!key.endsWith("/")) key += "/";
		if (key.equals("/")) key = "";
		
		ListObjectsV2Request req = new ListObjectsV2Request();
		req.setBucketName(bucketName);
		req.setPrefix(key);
		req.setDelimiter("/");
		ListObjectsV2Result res = s3.listObjectsV2(req);
		
		String originalKey = key;
		List<String> results = new ArrayList<>();
		results.addAll(res.getCommonPrefixes().stream().map(s -> getLocalName(s)).collect(Collectors.toList()));
		results.addAll(res.getObjectSummaries().stream()
				.filter(s -> !s.getKey().equals(originalKey))
				.map(s -> getLocalName(s.getKey())).collect(Collectors.toList()));
		return results;
	}

	@Override
	public void mkDir(String path) throws IOException {
		// Directories do not exist in S3.
	}

	@Override
	public void mkDirs(String path) throws IOException {
		// Directories do not exist in S3.
	}

	@Override
	public void delete(String path) throws IOException {
		s3.deleteObject(bucketName, getKey(path));
	}

	@Override
	public long getLength(String path) throws IOException {
		if (!exists(path)) return 0L;
		try (S3Object o = s3.getObject(bucketName, getKey(path))) {
			return o.getObjectMetadata().getContentLength();
		}
	}

	@Override
	public InputStream getInputStream(String path) throws IOException {
		S3Object o = s3.getObject(bucketName, getKey(path));
		return o.getObjectContent();
	}

	@Override
	public SeekableByteChannel getChannel(String path, String mode) throws IOException {
		if (mode.toLowerCase().contains("w")) throw new IOException("S3 interface does not support writable channels");
		return new CachingSeekableChannel(new SeekableS3Channel(s3, bucketName, getKey(path))) {
			@Override
			protected DataFetcher createDataFetcher(SeekableByteChannel delegate) {
				return (o,l) -> {
					byte[] data = new byte[l];
					((SeekableS3Channel) delegate).position(o, l);
					delegate.read(ByteBuffer.wrap(data));
					return data;
				};
			}
		};
	}

	@Override
	public File getAsFile(String path) {
		throw new RuntimeException("S3 interface does not support file handles");
	}

	@Override
	protected void doRenameTo(String from, String to) throws IOException {
		String keyFrom = getKey(from);
		String keyTo = getKey(to);
		s3.copyObject(bucketName, keyFrom, bucketName, keyTo);
		s3.deleteObject(bucketName, keyFrom);
	}
	
	@Override
	protected void doUpload(String path, InputStream input, long length) throws IOException {
		ObjectMetadata metadata = new ObjectMetadata();
		if (enableSSE) metadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
		if (length > 0) metadata.setContentLength(length);
		s3.putObject(bucketName, getKey(path), input, metadata);
	}
	
	private String getKey(String path) {
		if (path.startsWith("/")) path = path.substring(1);
		return path;
	}
	
	private String getLocalName(String key) {
		if (key.endsWith("/")) key = key.substring(0, key.length() - 1);
		return key.substring(key.lastIndexOf("/") + 1);
	}
}
