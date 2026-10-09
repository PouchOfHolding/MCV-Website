package pt.ualg.mcv.common.services;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Paths;

@Service
public class FileUrlService
{

    public String buildPublicUrl(String rawPath)
    {
        if (rawPath == null || rawPath.isBlank())
        {
            return null;
        }

        if (rawPath.startsWith("http://") || rawPath.startsWith("https://"))
        {
            return rawPath;
        }

        String normalizedPath = rawPath.replace("\\", "/");

        String relativePath;
        int uploadsIndex = normalizedPath.indexOf("uploads/");
        if (uploadsIndex != -1)
        {
            relativePath = normalizedPath.substring(uploadsIndex + "uploads/".length());
        }
        else
        {

            relativePath = Paths.get(normalizedPath).getFileName().toString();
        }

        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/uploads/")
                .path(relativePath).toUriString();
    }
}
