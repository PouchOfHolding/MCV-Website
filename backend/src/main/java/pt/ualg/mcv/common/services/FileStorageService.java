package pt.ualg.mcv.common.services;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import net.coobird.thumbnailator.Thumbnails;
import pt.ualg.mcv.exceptions.BadRequestException;
import pt.ualg.mcv.exceptions.InternalServerException;

@Service
public class FileStorageService
{

    private final Path rootLocation;

    private static final long MAX_FILE_SIZE = 500 * 1024L * 1024;

    private static final List<String> ALLOWED_MIME_TYPES =
            List.of("image/jpeg", "image/png", "image/gif", "image/webp");

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir)
    {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try
        {
            Files.createDirectories(this.rootLocation);
        }
        catch (IOException e)
        {
            throw new InternalServerException();
        }
    }

    public String storeFile(MultipartFile file)
    {
        validateImage(file);

        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains("."))
        {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFileName = UUID.randomUUID() + fileExtension;

        try
        {
            Path targetLocation = this.rootLocation.resolve(uniqueFileName).normalize();

            if (!targetLocation.startsWith(this.rootLocation))
            {
                throw new BadRequestException("Cannot store file outside current directory.");
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return targetLocation.toString();
        }
        catch (IOException e)
        {
            throw new InternalServerException();
        }
    }

    public void deleteFile(String filePathStr)
    {
        if (filePathStr == null || filePathStr.isBlank())
        {
            return;
        }

        try
        {
            Path filePath = Paths.get(filePathStr);
            Files.deleteIfExists(filePath);
        }
        catch (IOException e)
        {
            throw new InternalServerException();
        }
    }

    public String createThumbnail(String originalFilePath)
    {
        try
        {
            Path originalPath = Paths.get(originalFilePath);
            String fileName = originalPath.getFileName().toString();

            String thumbFileName = "thumb_" + UUID.randomUUID() + "_" + fileName;

            Path thumbTargetLocation = this.rootLocation.resolve(thumbFileName).normalize();

            Thumbnails.of(originalPath.toFile()).size(200, 200)
                    .toFile(thumbTargetLocation.toFile());

            return thumbTargetLocation.toString();
        }
        catch (IOException e)
        {
            return null;
        }
    }

    private void validateImage(MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            throw new BadRequestException("Cannot save an empty file.");
        }

        if (file.getSize() > MAX_FILE_SIZE)
        {
            throw new BadRequestException("File size exceeds the maximum allowed limit of "
                    + (MAX_FILE_SIZE / (1024 * 1024)) + " MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase()))
        {
            throw new BadRequestException(
                    "Invalid file type. Only JPEG, PNG, GIF, and WEBP images are allowed.");
        }

        try (InputStream inputStream = file.getInputStream())
        {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null)
            {
                throw new BadRequestException("Uploaded file is corrupt or not a valid image.");
            }
        }
        catch (IOException e)
        {
            throw new InternalServerException();
        }
    }
}
