package org.lecturestudio.web.portal.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.lecturestudio.web.portal.exception.FileStorageException;
import org.lecturestudio.web.portal.property.FileStorageProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

	private final Path storageLocation;


	@Autowired
	public FileStorageService(FileStorageProperties storageProperties) {
		storageLocation = Paths.get(storageProperties.getUploadDir()).toAbsolutePath().normalize();

		try {
			Files.createDirectories(storageLocation);
		}
		catch (Exception e) {
			throw new FileStorageException("Could not create the fire storage directory", e);
		}
	}

	public String save(MultipartFile file) {
		// Normalize file name.
		String fileName = StringUtils.cleanPath(file.getOriginalFilename());
		Path targetLocation = storageLocation.resolve(fileName).normalize();

		// Prevent path-traversal.
		if (!targetLocation.startsWith(storageLocation)) {
			throw new FileStorageException("Filename contains invalid path sequence");
		}

		try {
			Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException e) {
			throw new FileStorageException("Could not store file " + fileName, e);
		}

		return fileName;
	}

	public Resource load(String fileName) {
		Path filePath = storageLocation.resolve(fileName).normalize();

		try {
			Resource resource = new UrlResource(filePath.toUri());

			if (resource.exists() || resource.isReadable()) {
				return resource;
			}
		}
		catch (MalformedURLException e) {
			throw new FileStorageException("File not found " + fileName, e);
		}

		throw new FileStorageException("File not found " + fileName);
	}

	public void deleteAll() {
		FileSystemUtils.deleteRecursively(storageLocation.toFile());
	}

}
