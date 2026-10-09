package pt.ualg.mcv.family;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyHasMemberId implements Serializable {

    @Column(name = "family_uuid")
    private UUID familyUuid;

    @Column(name = "member_uuid")
    private UUID memberUuid;
}