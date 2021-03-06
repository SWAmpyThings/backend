package domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ActivitySummary {
    private BigDecimal totalGreenRewardPointsEarned;
    private BigDecimal totalGreenRewardPointsAvailable;
    private BigDecimal totalLifetimePoints;
}
