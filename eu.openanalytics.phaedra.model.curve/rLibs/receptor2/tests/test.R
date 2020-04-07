# TODO: Add comment
# 
# Author: Vahid Nassiri
###############################################################################
library(receptor2)
## Importing Excel files
library(openxlsx)
cellularAssay <- openxlsx::read.xlsx(xlsxFile = system.file("extdata", "191212_ELISA plate_oldDNA_wk50_Welldata.xlsx", package="receptor2"), sheet = 1)
biochemicalAssay <- openxlsx::read.xlsx(xlsxFile = system.file("extdata", "190821_compound assay Plate_384_wk34_Welldata.xlsx", package="receptor2"), sheet = 1)
cellularHighContentAssay <- openxlsx::read.xlsx(xlsxFile = system.file("extdata", "MKL1_wk5_Welldata.xlsx", package="receptor2"), sheet = 1)
## Making suitabel data out of the imported Excel files
cellularAssayData <- makingDoseResponseData(cellularAssay)
biochemicalAssayData <- makingDoseResponseData(biochemicalAssay)
### Note that for this case we have two datasets per compound.
cellularHighContentAssayData <- makingDoseResponseData(cellularHighContentAssay)
## if we want to keep the replicates via different plates we may use the following codes
cellularAssayDataNoAggregating <- makingDoseResponseData(cellularAssay, aggregatePlates = FALSE)
biochemicalAssayDataNoAggregating <- makingDoseResponseData(biochemicalAssay, aggregatePlates = FALSE)
### Note that for this case we have two datasets per compound.
cellularHighContentAssayDataNoAggregating <- makingDoseResponseData(cellularHighContentAssay, aggregatePlates = FALSE)
## Fitting logistic model to the data
cellularAssayResultsNoAggregation <- lapply(cellularAssayDataNoAggregating, fittingLogisticModel, 
		fixedBottom = NA, fixedTop = NA, fixedSlope = NA, confLevel = 0.95, 
		robustMethod = "mean", responseName = "PIN.pos.median", slope = "free", validRespRange = 30)
### Checking one of the results
names(cellularAssayResultsNoAggregation[[1]])
cellularAssayResultsNoAggregation[[1]]$validpIC50 # vlaid pIC50 (after applying the checks)
cellularAssayResultsNoAggregation[[1]]$checkpIC50 # check results for pIC50
cellularAssayResultsNoAggregation[[1]]$messagepIC50 # in case estimated pIC50 is smaller than min-dose or second-min-dose
cellularAssayResultsNoAggregation[[1]]$validpIC20 # vlaid pIC20 (after applying the checks)
cellularAssayResultsNoAggregation[[1]]$checkpIC20 # check results for pIC20
cellularAssayResultsNoAggregation[[1]]$messagepIC20 # in case estimated pIC20 is smaller than min-dose or second-min-dose
cellularAssayResultsNoAggregation[[1]]$validpIC80 # vlaid pIC80 (after applying the checks)
cellularAssayResultsNoAggregation[[1]]$checkpIC80 # check results for pIC80
cellularAssayResultsNoAggregation[[1]]$messagepIC80 # in case estimated pIC80 is smaller than min-dose or second-min-dose
cellularAssayResultsNoAggregation[[1]]$modelCoefs # parameter estimates with their standard error, significance tests and confidence intervals
cellularAssayResultsNoAggregation[[1]]$residulaVariance # estimated model residual variance 
cellularAssayResultsNoAggregation[[1]]$warningFit	 # check for possible warnings
cellularAssayResultsNoAggregation[[1]]$fittedModel # a drc object of the fitted model
cellularAssayResultsNoAggregation[[1]]$plot # plotting the data, model and pIC50 (if any possible)
cellularAssayResultsNoAggregation[[1]]$slopeWarning # a warning regarding the result of checking the slope
cellularAssayResultsNoAggregation[[1]]$rangeResults # eMin and eMax and their corresponding doses
cellularAssayResultsNoAggregation[[1]]$xIC # AIC and BIC of the fitted Model
## Note that, warningFit now shows the information about the model fit. For this example the possible outcomes with their frequencies are:
table(unlist(lapply(cellularAssayResultsNoAggregation,"[[", 13)))
## Now if we do it for the same data but we aggregating we may see there are only one point per dose
cellularAssayResults <- lapply(cellularAssayData, fittingLogisticModel, 
		fixedBottom = NA, fixedTop = NA, fixedSlope = NA, confLevel = 0.95, 
		robustMethod = "mean", responseName = "PIN.pos.median", slope = "free", validRespRange = 30)
cellularAssayResults[[1]]$plot 
## Now we may test various options in the fittingLogisticModel function
### inputData, fixedBottom = NA, fixedTop = NA, fixedSlope = NA, confLevel = 0.95, 
#		robustMethod = c("mean", "median"), responseName, slope = c("free", "ascending","descending"), validRespRange = 30, 
#		receptorStyleModel = c(NULL, "PL4", "PL3L", "PL3U", "2PL", "PL4H1", "PL3LH1", "PL3UH1", "PL2H1")

### Fixing bottom to 0
fitFixedBottom <- fittingLogisticModel(inputData = cellularAssayData[[1]], fixedBottom = 0, fixedTop = NA, 
		fixedSlope = NA, confLevel = 0.95, robustMethod = "mean", responseName = "PIN.pos.median", slope = "free", 
		validRespRange = 30)
### Fixing bottom to 0 with receptor style, note that we need to specify that fixed lower bottom, otherwise it will give an error:
fitFixedBottomReceptor <- fittingLogisticModel(inputData = cellularAssayData[[1]], fixedBottom = NA, fixedTop = NA, 
		fixedSlope = NA, confLevel = 0.95, robustMethod = "mean", responseName = "PIN.pos.median", slope = "free", 
		validRespRange = 30, receptorStyleModel = "PL3L")
### Now we provide that fixed value, and it should produce the same results as in fitFixedBottom
fitFixedBottomReceptor <- fittingLogisticModel(inputData = cellularAssayData[[1]], fixedBottom = 0, fixedTop = NA, 
		fixedSlope = NA, confLevel = 0.95, robustMethod = "mean", responseName = "PIN.pos.median", slope = "free", 
		validRespRange = 30, receptorStyleModel = "PL3L")
fitFixedBottomReceptor$modelCoefs
fitFixedBottom$modelCoefs
### We may now try to fix the slope to 1 using receptor style, as one may see, when specifying the PL4H1, then even having
### fixedBottom= 0 does not matter, as receptorStyleModel always comes first.
fitFixedSlopeReceptor <- fittingLogisticModel(inputData = cellularAssayData[[1]], fixedBottom = 0, fixedTop = NA, 
		fixedSlope = NA, confLevel = 0.95, robustMethod = "mean", responseName = "PIN.pos.median", slope = "free", 
		validRespRange = 30, receptorStyleModel = "PL4H1")
### Now lets get the same model using drc style
fitFixedSlope <- fittingLogisticModel(inputData = cellularAssayData[[1]], fixedBottom = NA, fixedTop = NA, 
		fixedSlope = 1, confLevel = 0.95, robustMethod = "mean", responseName = "PIN.pos.median", slope = "free", 
		validRespRange = 30)
### They should give the same ressults
fitFixedSlope$modelCoefs
fitFixedSlopeReceptor$modelCoefs
### 
fitCheckSlope <- fittingLogisticModel(inputData = cellularAssayData[[61]], fixedBottom = NA, fixedTop = NA, 
		fixedSlope = NA, confLevel = 0.95, robustMethod = "mean", responseName = "PIN.pos.median", slope = "free", 
		validRespRange = 30)
fitCheckSlope$modelCoefs[1,]
### As we may see, the slope is estimated negative here, i.e., a descending curve, now if we set slope = "ascending" no model should be fitted to it
fitAscending <- fittingLogisticModel(inputData = cellularAssayData[[61]], fixedBottom = NA, fixedTop = NA, 
		fixedSlope = NA, confLevel = 0.95, robustMethod = "mean", responseName = "PIN.pos.median", slope = "ascending", 
		validRespRange = 30)
fitAscending$plot
## We may also see the situation of the model: could be fitted or not, if yes, how was its covariance matrix, if not, why was that.
table(unlist(lapply(cellularAssayResults,"[[", 13)))
### Now we can find the two compounds with not a wide enough range
which(unlist(lapply(cellularAssayResults,"[[", 13)) == "The response range is below required value.")
### Let's check one of them: as we may see the range is  below 30
max(cellularAssayData[[46]]$response) - min(cellularAssayData[[46]]$response)
### Let us check the fit
fitNotWideEnough <- fittingLogisticModel(inputData = cellularAssayData[[46]], fixedBottom = NA, fixedTop = NA, 
		fixedSlope = NA, confLevel = 0.95, robustMethod = "mean", responseName = "PIN.pos.median", slope = "free", 
		validRespRange = 30)
fitNotWideEnough$warningFit
fitNotWideEnough$pIC50merged
## we also can see in the plot that all of the responses are below 50, so it is an inactive compound. 