package pt.ualg.mcv.family;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "family_has_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyHasMember {

    @EmbeddedId
    private FamilyHasMemberId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("familyUuid")
    @JoinColumn(name = "family_uuid")
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("memberUuid")
    @JoinColumn(name = "member_uuid")
    private Member member;

    @Enumerated(EnumType.STRING)
    private FamilyMemberRole role;
}
