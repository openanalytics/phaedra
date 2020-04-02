# TODO: Add comment
# 
# Author: Vahid Nassiri
###############################################################################

## Importing Excel files
library(openxlsx)
library(receptor2)
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
		robustMethod = "mean", responseName = "PIN.pos.median")
### Checking one of the results
names(cellularAssayResultsNoAggregation[[1]])
cellularAssayResultsNoAggregation[[1]]$validpIC50 # vlaid pIC50 (after applying the checks)
cellularAssayResultsNoAggregation[[1]]$modelCoefs # parameter estimates with their standard error, significance tests and confidence intervals
cellularAssayResultsNoAggregation[[1]]$residulaVariance # estimated model residual variance 
cellularAssayResultsNoAggregation[[1]]$warningPD	 # check if the covariance matrix of the estimated parameters is positive-definite
cellularAssayResultsNoAggregation[[1]]$fittedModel # a drc object of the fitted model
cellularAssayResultsNoAggregation[[1]]$plot # plotting the data, model and pIC50 (if any possible)
## Now if we do it for the same data but we aggregating we may see there are only one point per dose
cellularAssayResults <- lapply(cellularAssayData, fittingLogisticModel, 
		fixedBottom = NA, fixedTop = NA, fixedSlope = NA, confLevel = 0.95, 
		robustMethod = "mean", responseName = "PIN.pos.median")
cellularAssayResults[[1]]$plot 
## It is also interesting to consider 5hose 5 warning message stating: In sqrt(diag(varMat)) : NaNs produced
## they are there since some mdoels are estimated with a non-PD covariance matrix. We can also find them:
which(unlist(lapply(cellularAssayResults,"[[", 4) != "Covariance matrix is positive definite."))
