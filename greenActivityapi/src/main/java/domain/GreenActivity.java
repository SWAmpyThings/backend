package domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GreenActivity {
    String rapidRewardsNumber;
    String recordLocator;
    String bound;
    Boolean isElectronicBoardingPass;
    Integer checkedBags;
    String activityDate;
}
