package de.unijena.bioinf.ms.persistence.model.core.networks;

import jakarta.persistence.Id;
import lombok.*;

import java.util.List;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AdductNetwork {

    @Id
    private long networkId;
    private List<AdductNode> nodes;
    private List<AdductEdge> egdes;
}
