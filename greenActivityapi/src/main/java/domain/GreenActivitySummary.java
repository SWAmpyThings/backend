package domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GreenActivitySummary {
    Integer electronicBoardingPassRewards;
    Integer noCheckedBagRewards;
    Integer lightCarryOnRewards;
    Integer greenIdeasRewards;
    Integer approvedGreenIdeasRewards;

    @Override
    public String toString() {
        return String.format("{\"electronicBoardingPassRewards\": %d, \"noCheckedBagRewards\": %d, " +
                        "\"lightCarryOnRewards\": %d, \"greenIdeasRewards\": %d, \"approvedGreenIdeasRewards\": %d }",
                electronicBoardingPassRewards,
                noCheckedBagRewards,
                lightCarryOnRewards,
                greenIdeasRewards,
                approvedGreenIdeasRewards);
    }
}
