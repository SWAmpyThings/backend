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

    @Override
    public String toString() {
        return String.format("GreenActivitySummary: {electronicBoardingPassRewards: %d, noCheckedBagRewards: %d, lightCarryOnRewards: %d }",
                electronicBoardingPassRewards,
                noCheckedBagRewards,
                lightCarryOnRewards);
    }
}
