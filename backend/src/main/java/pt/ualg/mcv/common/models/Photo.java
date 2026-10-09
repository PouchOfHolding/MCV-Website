package pt.ualg.mcv.common.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import pt.ualg.mcv.user.User;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    protected UUID uuid;

    @NotBlank
    @Column(nullable = false, name = "original_name")
    protected String originalName;

    @NotBlank
    @Column(nullable = false, name = "file_path")
    protected String filePath;

    @Column(name = "thumbnail_path")
    protected String thumbnailPath;

    @Positive
    @Column(nullable = false, name = "file_size_bytes")
    protected long fileSizeBytes;

    @NotBlank
    @Column(nullable = false, name = "mime_type", length = 100)
    protected String mimeType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_uuid", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User createdBy;
    
    @CreationTimestamp
    @Column(nullable = false, name = "created_at", updatable = false)
    protected Instant createdAt;
}