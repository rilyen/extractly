package com._6.extractly.model;

public class zohoForm {
    // Text fields to be filled in Zoho Form
    private String integration;
    private String assignedDesigner;
    private String dealName;
    private String resources;
    private String projectID;
    private String designLink;
    private String companyWebsite;
    private String companyAbout;
    private String productPurpose;
    private String projectCostModifier;
    private String stage;
    private String designDocuments;
    private String allocatedBudget;
    private String usedHours;
    private String projectClass;
    private String productionNotes;
    private String customerConcerns;
    private String qcTurnaroundTime;
    private String pulledSOWEstimatedTime;
    private String devEstimatedTime;

    // Date fields (kept as String in YYYY-MM-DD; parse later if needed)
    private String devStartDate;
    private String designDueDate;
    private String designCompletionDate;
    private String pullClosingDate;
    private String projectedDeliveryDate;
    private String negotiatedDeliveryDueDate;
    private String projectedCompletionDate;

    // Boolean / checkbox fields
    private Boolean reviewProjectDocs;
    private Boolean draftRoadmap;
    private Boolean customerFeedback;
    private Boolean updateRoadmap;
    private Boolean createUpdateUserstories;
    private Boolean internalReview;
    private Boolean submittedForApproval;
    private Boolean onTrackIATs;
    private Boolean onTrackEmailsVideos;
    private Boolean flagProblem;
    private Boolean fullProjectIAT;
    private Boolean qcTestingCompleted;
    private Boolean demoVideoRecorded;
    private Boolean editPackageVideo;
    private Boolean cleanUpDatabase;

    
    public String getIntegration() {
        return integration;
    }
    public void setIntegration(String integration) {
        this.integration = integration;
    }
    public String getAssignedDesigner() {
        return assignedDesigner;
    }
    public void setAssignedDesigner(String assignedDesigner) {
        this.assignedDesigner = assignedDesigner;
    }
    public String getDealName() {
        return dealName;
    }
    public void setDealName(String dealName) {
        this.dealName = dealName;
    }
    public String getResources() {
        return resources;
    }
    public void setResources(String resources) {
        this.resources = resources;
    }
    public String getProjectID() {
        return projectID;
    }
    public void setProjectID(String projectID) {
        this.projectID = projectID;
    }
    public String getDesignLink() {
        return designLink;
    }
    public void setDesignLink(String designLink) {
        this.designLink = designLink;
    }
    public String getCompanyWebsite() {
        return companyWebsite;
    }
    public void setCompanyWebsite(String companyWebsite) {
        this.companyWebsite = companyWebsite;
    }
    public String getCompanyAbout() {
        return companyAbout;
    }
    public void setCompanyAbout(String companyAbout) {
        this.companyAbout = companyAbout;
    }
    public String getProductPurpose() {
        return productPurpose;
    }
    public void setProductPurpose(String productPurpose) {
        this.productPurpose = productPurpose;
    }
    public String getProjectCostModifier() {
        return projectCostModifier;
    }
    public void setProjectCostModifier(String projectCostModifier) {
        this.projectCostModifier = projectCostModifier;
    }
    public String getStage() {
        return stage;
    }
    public void setStage(String stage) {
        this.stage = stage;
    }
    public String getDesignDocuments() {
        return designDocuments;
    }
    public void setDesignDocuments(String designDocuments) {
        this.designDocuments = designDocuments;
    }
    public String getAllocatedBudget() {
        return allocatedBudget;
    }
    public void setAllocatedBudget(String allocatedBudget) {
        this.allocatedBudget = allocatedBudget;
    }
    public String getUsedHours() {
        return usedHours;
    }
    public void setUsedHours(String usedHours) {
        this.usedHours = usedHours;
    }
    public String getProjectClass() {
        return projectClass;
    }
    public void setProjectClass(String projectClass) {
        this.projectClass = projectClass;
    }
    public String getProductionNotes() {
        return productionNotes;
    }
    public void setProductionNotes(String productionNotes) {
        this.productionNotes = productionNotes;
    }
    public String getCustomerConcerns() {
        return customerConcerns;
    }
    public void setCustomerConcerns(String customerConcerns) {
        this.customerConcerns = customerConcerns;
    }
    public String getQcTurnaroundTime() {
        return qcTurnaroundTime;
    }
    public void setQcTurnaroundTime(String qcTurnaroundTime) {
        this.qcTurnaroundTime = qcTurnaroundTime;
    }
    public String getPulledSOWEstimatedTime() {
        return pulledSOWEstimatedTime;
    }
    public void setPulledSOWEstimatedTime(String pulledSOWEstimatedTime) {
        this.pulledSOWEstimatedTime = pulledSOWEstimatedTime;
    }
    public String getDevEstimatedTime() {
        return devEstimatedTime;
    }
    public void setDevEstimatedTime(String devEstimatedTime) {
        this.devEstimatedTime = devEstimatedTime;
    }
    public String getDevStartDate() {
        return devStartDate;
    }
    public void setDevStartDate(String devStartDate) {
        this.devStartDate = devStartDate;
    }
    public String getDesignDueDate() {
        return designDueDate;
    }
    public void setDesignDueDate(String designDueDate) {
        this.designDueDate = designDueDate;
    }
    public String getDesignCompletionDate() {
        return designCompletionDate;
    }
    public void setDesignCompletionDate(String designCompletionDate) {
        this.designCompletionDate = designCompletionDate;
    }
    public String getPullClosingDate() {
        return pullClosingDate;
    }
    public void setPullClosingDate(String pullClosingDate) {
        this.pullClosingDate = pullClosingDate;
    }
    public String getProjectedDeliveryDate() {
        return projectedDeliveryDate;
    }
    public void setProjectedDeliveryDate(String projectedDeliveryDate) {
        this.projectedDeliveryDate = projectedDeliveryDate;
    }
    public String getNegotiatedDeliveryDueDate() {
        return negotiatedDeliveryDueDate;
    }
    public void setNegotiatedDeliveryDueDate(String negotiatedDeliveryDueDate) {
        this.negotiatedDeliveryDueDate = negotiatedDeliveryDueDate;
    }
    public String getProjectedCompletionDate() {
        return projectedCompletionDate;
    }
    public void setProjectedCompletionDate(String projectedCompletionDate) {
        this.projectedCompletionDate = projectedCompletionDate;
    }
    public Boolean getReviewProjectDocs() {
        return reviewProjectDocs;
    }
    public void setReviewProjectDocs(Boolean reviewProjectDocs) {
        this.reviewProjectDocs = reviewProjectDocs;
    }
    public Boolean getDraftRoadmap() {
        return draftRoadmap;
    }
    public void setDraftRoadmap(Boolean draftRoadmap) {
        this.draftRoadmap = draftRoadmap;
    }
    public Boolean getCustomerFeedback() {
        return customerFeedback;
    }
    public void setCustomerFeedback(Boolean customerFeedback) {
        this.customerFeedback = customerFeedback;
    }
    public Boolean getUpdateRoadmap() {
        return updateRoadmap;
    }
    public void setUpdateRoadmap(Boolean updateRoadmap) {
        this.updateRoadmap = updateRoadmap;
    }
    public Boolean getCreateUpdateUserstories() {
        return createUpdateUserstories;
    }
    public void setCreateUpdateUserstories(Boolean createUpdateUserstories) {
        this.createUpdateUserstories = createUpdateUserstories;
    }
    public Boolean getInternalReview() {
        return internalReview;
    }
    public void setInternalReview(Boolean internalReview) {
        this.internalReview = internalReview;
    }
    public Boolean getSubmittedForApproval() {
        return submittedForApproval;
    }
    public void setSubmittedForApproval(Boolean submittedForApproval) {
        this.submittedForApproval = submittedForApproval;
    }
    public Boolean getOnTrackIATs() {
        return onTrackIATs;
    }
    public void setOnTrackIATs(Boolean onTrackIATs) {
        this.onTrackIATs = onTrackIATs;
    }
    public Boolean getOnTrackEmailsVideos() {
        return onTrackEmailsVideos;
    }
    public void setOnTrackEmailsVideos(Boolean onTrackEmailsVideos) {
        this.onTrackEmailsVideos = onTrackEmailsVideos;
    }
    public Boolean getFlagProblem() {
        return flagProblem;
    }
    public void setFlagProblem(Boolean flagProblem) {
        this.flagProblem = flagProblem;
    }
    public Boolean getFullProjectIAT() {
        return fullProjectIAT;
    }
    public void setFullProjectIAT(Boolean fullProjectIAT) {
        this.fullProjectIAT = fullProjectIAT;
    }
    public Boolean getQcTestingCompleted() {
        return qcTestingCompleted;
    }
    public void setQcTestingCompleted(Boolean qcTestingCompleted) {
        this.qcTestingCompleted = qcTestingCompleted;
    }
    public Boolean getDemoVideoRecorded() {
        return demoVideoRecorded;
    }
    public void setDemoVideoRecorded(Boolean demoVideoRecorded) {
        this.demoVideoRecorded = demoVideoRecorded;
    }
    public Boolean getEditPackageVideo() {
        return editPackageVideo;
    }
    public void setEditPackageVideo(Boolean editPackageVideo) {
        this.editPackageVideo = editPackageVideo;
    }
    public Boolean getCleanUpDatabase() {
        return cleanUpDatabase;
    }
    public void setCleanUpDatabase(Boolean cleanUpDatabase) {
        this.cleanUpDatabase = cleanUpDatabase;
    }

}