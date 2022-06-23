library(dynamicTreeCut)
library(stringr)
library(cluster)
library(clValid)
library(dplyr)
library(parallel)
library(reticulate)
library(knitr)
library(rmarkdown)

source("generate_md.R")
source("selection/frequency_selection.R")

# Loading the Python script for the highest indices
source_python("scripts/local_bumps.py")

data.information <- data.frame()
generated.md <- 0
valid.features <- 0


process.data <- function(library.name) {
  
  ######################### IMPORTANT   ################################### 
  # To save processing time of the data, some pre-processed serialised    #
  # files have been shared and they are the default input to the rest     #
  # of the processing in this file. In case you want to re-compaute the   #
  # results again from scratch or desire add another library, the process #
  # step is in the file "scripts/extract-new-features.R". You only have   #
  # to substitute the lines about reading the serialised files with the   # 
  # code in the file.                                                     #
  #########################################################################
  
  # Name of the file where results are stored
  file.results <- str_c(getwd(), "/serial/", library.name, ".rds")
  
  # Loading the previously serialised results
  list.results <- readRDS(file.results)
  
  # hierarchical.data <- list.results[[1]]
  data.information <- list.results[[2]]
  
  return(data.information)
}


classes.methods.calls <- function(cluster, unique = F) {
  all.classes <- c()
  all.methods <- c()
  all.calls <- c()
  
  for (usage in cluster) {
    cleaned.calls <- strsplit(usage, "<br>")[[1]]
    all.calls <- c(all.calls, cleaned.calls)
    
    classes.usages <- c()
    methods.usages <- c()
    
    for (call in cleaned.calls) {
      call.divided <- strsplit(call, "\\.")[[1]]
      
      classes.usages <- c(classes.usages, call.divided[1])
      methods.usages <- c(methods.usages, call.divided[2:length(call.divided)])
    }
    
    unique.classes.usages <- unique(classes.usages)
    unique.methods.usages <- unique(methods.usages)
    
    if (unique) {
      all.classes <- c(all.classes, unique.classes.usages)
      all.methods <- c(all.methods, unique.methods.usages)
    } else {
      all.classes <- c(all.classes, classes.usages)
      all.methods <- c(all.methods, methods.usages)
    }
  }
  
  list(classes=all.classes, methods=all.methods, calls=all.calls)
}

obtain.frequency <- function(data.calls) {
  # Extracting frequencies from the calls
  classes.methods.calls.list <- classes.methods.calls(data.calls)
  
  all.classes <- classes.methods.calls.list$classes
  all.methods <- classes.methods.calls.list$methods
  all.calls <- classes.methods.calls.list$calls
  
  unique.classes <- unique(all.classes)
  unique.methods <- unique(all.methods)
  
  freq.classes <- unlist(lapply(unique.classes, function(class.name) length(which(all.classes == class.name))))
  freq.methods <- unlist(lapply(unique.methods, function(method.name) length(which(all.methods == method.name))))
  
  percent.classes <- round(freq.classes / length(data.calls) * 100, digits = 2)
  percent.classes <- unlist(lapply(percent.classes, function(freq) if (freq > 100) 100 else freq))
  percent.methods <- round(freq.methods / length(data.calls) * 100, digits = 2)
  percent.methods <- unlist(lapply(percent.methods, function(freq) if (freq > 100) 100 else freq))
  
  # Getting the precedents of the method calls
  precedents <- c()
  for (method.call in unique.methods) {
    precedents.method_call <- c()
    
    for (call in all.calls) {
      call.divided <- strsplit(call, "\\.")[[1]]
      
      if (method.call %in% call.divided) {
        indexes.method_call <- which(call.divided == method.call)
        
        for (index in indexes.method_call) {
          precedents.method_call <- c(precedents.method_call, paste(call.divided[1:index - 1], collapse = "."))
        }
      }
    }
    precedents <- c(precedents, paste(unique(precedents.method_call), collapse = ","))
  }
  
  df.classes <- data.frame(unique.classes, freq.classes, percent.classes)
  df.methods <- data.frame(unique.methods, freq.methods, percent.methods, precedents)
  
  # Order data frames
  ordered.classes <- df.classes[order(freq.classes, decreasing = T),]
  ordered.methods <- df.methods[order(freq.methods, decreasing = T),]
  
  return(list("classes" = ordered.classes, "methods" = ordered.methods))
}

# Generate the books
to.markdowns <- function(data.information, library) {
  get.distinctions <- function(name.cluster) {
    filtered.data <- filter(data.information, group == name.cluster)
    filtered.data_names <- filtered.data$name
    
    filtered.data_calls <- filtered.data$calls
    results.frequent <- obtain.frequency(filtered.data_calls)
    
    # Frequent APIs
    data.api_calls.ord <- make.apis.df(filtered.data_calls)
    code.feature <- frequent.apis(results.frequent$methods$freq.methods, data.api_calls.ord)
    
    joined.names <- paste(filtered.data_names, collapse = ",")
    all.names <- strsplit(joined.names, ",")[[1]]
    selected.names <- cluster.name.titles(all.names)
    
    return(list(code.feature, selected.names))
  }
  
  unique.features <- unique(data.information$group)
  
  # Parallel processing
  print("Parallel processing of features ...")
  results.distinctions <- mclapply(unique.features, get.distinctions)
  results.distinctions <- Filter(function(element) !is.null(element[[1]]), results.distinctions)
  print("Done!")
  
  # Extract the data from the lists
  distinction.code <- lapply(results.distinctions, function(result) result[[1]])
  names.code <- lapply(results.distinctions, function(result) result[[2]])

  generate.mds(library, distinction.code, names.code)
}


################################ App ################################

libraries = c("guava", "httpclient", "itextpdf", "jfreechart", "jsoup", "pdfbox", "poi-ooxml", "quartz")

for (library in libraries) {
  cat(sprintf("Processing library: %s\n", library))
  data <- process.data(library)
  to.markdowns(data, library)
}

