##########################################################################
#                              IMPORTANT                                 #
##########################################################################
# This code should be included and executed to extract new features from #
# a library. More specifically put this code in the processing.R file at #
# the beginning of function output$clustersTreeMapUI. The current imple- #
# mentation reads from serialised data.                                  #  
##########################################################################

# Selecting the input for an specific library and combination
# Data reading
data.path <- str_c(getwd(), "/datasets/input/", input$library)
data.tags <- read.csv(str_c(data.path, "/similarity_matrix.csv"), header = F)

data.usages <- readLines(str_c(data.path, "/code_usages.txt"))
name.candidates <- readLines(str_c(data.path, "/name_candidates.txt"))
titles.information <- readLines(str_c(data.path, "/extra_information.txt"))

# Getting the similarities from the data
data.matrix <- as.matrix(data.tags)
sim <- data.matrix / sqrt(rowSums(data.matrix * data.matrix))
sim <- sim %*% t(sim)
d_sim <- as.dist(1 - sim)
d_sim[is.na(d_sim)] <- 0.0

# Clustering the data
dendrogram <- hclust(d_sim, method = "average")

# Cutting the dendrogram
cut.results <- cutreeHybrid(dendro = dendrogram, distM = as.matrix(d_sim),
                            minClusterSize = 1, verbose = 0)

# Inspecting the clusters
cluster.labels <- unname(cut.results$labels)
unique.labels <- unique(cluster.labels)

# Get the usages for each cluster
get.labels <- function(index.label) {
  indexes.cluster <- which(cluster.labels == index.label)
  labels.cluster <- unname(data.usages[indexes.cluster])
  
  # Join all usages
  all.labels <- str_c(labels.cluster, collapse = " ")
  
  return(all.labels)
}

# Get the names for each cluster
get.names <- function(index.label) {
  indexes.cluster <- which(cluster.labels == index.label)
  names.cluster <- unname(name.candidates[indexes.cluster])
  
  # Join all names
  all.names <- str_c(names.cluster, collapse = " ")
  
  return(all.names)
}

# Get the links of titles
get.links <- function(index.label) {
  indexes.cluster <- which(cluster.labels == index.label)
  information.cluster <- unname(titles.information[indexes.cluster])
  
  # Making link elements
  links <- c()
  for (information.title in information.cluster) {
    information.divided <- strsplit(information.title, "-->")[[1]]
    title.post <- information.divided[1]
    link.post <- information.divided[2]
    
    link <- str_c('<a href="',link.post,'">',title.post,'</a>')
    links <- c(links, link)
  }
  
  return(links)
}

# Make document composed by other documents for the usages
document.corpus <- get.labels(unique.labels[1])
for (iter in c(2 : length(unique.labels))) {
  document.corpus <- c(document.corpus, get.labels(unique.labels[iter]))
}

# Make document composed by other documents for the names
document.names <- get.names(unique.labels[1])
for (iter in c(2 : length(unique.labels))) {
  document.names <- c(document.names, get.names(unique.labels[iter]))
}

# API Usage information
cluster.group <- c()
cluster.subgroup <- c()
value.length <- c()
value.calls <- c()
value.name <- c()
title.links <- c()

k <- 1
index <- 1

for (document in document.corpus) {
  cluster.sentence <- strsplit(document, " ")[[1]]
  usages.subgroup <- c()
  length.cluster <- c()
  
  for (usage in cluster.sentence) {
    usage.replaced <- str_replace_all(usage, ",", "<br>")
    usages.subgroup <- c(usages.subgroup, usage.replaced)
    
    usage.divided <- strsplit(usage, ",")[[1]]
    length.cluster <- c(length.cluster, length(usage.divided))
  }
  
  # Saving only those groups that are selected by LOF
  frequency.calls <- obtain.frequency(usages.subgroup)
  frequency.methods <- frequency.calls$methods$freq.methods
  
  if (length(frequency.methods) > 1) {
    outliers <- get_outliers(frequency.methods)
    
    if (length(outliers) == 0) {
      outliers <- get_outliers(frequency.methods, 1)
    }
    
    if (length(outliers) > 0) {
      value.length <- c(value.length, length.cluster)
      value.calls <- c(value.calls, usages.subgroup)
      
      # Getting the core and branches of the code snippets
      data.api_calls.ord <- make.apis.df(usages.subgroup)
      selected.apis <- frequent.apis(frequency.calls$methods$freq.methods, data.api_calls.ord, F)
      
      usages.depicted <- highlight.apis(selected.apis, usages.subgroup)
      cluster.subgroup <- c(cluster.subgroup, usages.depicted)
      
      name.sentence <- strsplit(document.names[index], " ")[[1]]
      joined.names <- paste(name.sentence, collapse = ",")
      all.names <- strsplit(joined.names, ",")[[1]]
      
      # Names with LOF
      selected.terms.list <- cluster.name.titles(all.names)
      
      title.list <- selected.terms.list[[1]]
      body.list <- selected.terms.list[[2]]
      
      domain.body.df <- body.list[[1]] # Nouns
      action.body.df <- body.list[[2]] # Verbs
      
      # Ordered (in descending order) data by frequency
      domain.body.ord.df <- domain.body.df[order(-domain.body.df$freq),]
      action.body.ord.df <- action.body.df[order(-action.body.df$freq),]
      
      if (length(title.list) > 0) {
        domain.title.df <- title.list[[1]] # Nouns
        action.title.df <- title.list[[2]] # Verbs
        
        domain.title.ord.df <- domain.title.df[order(-domain.title.df$freq),]
        action.title.ord.df <- action.title.df[order(-action.title.df$freq),]
        
        name.group <- str_c("Action Title: ", head(action.title.ord.df, n = 1)$term, "<br>", 
                            "Domain Title: ", head(domain.title.ord.df, n = 1)$term, "<br><br>",
                            "Action Body: ", head(action.body.ord.df, n = 1)$term, "<br>", 
                            "Domain Body: ", head(domain.body.ord.df, n = 1)$term)
      } else {
        name.group <- str_c("Action Body: ", head(action.body.ord.df, n = 1)$term, "<br>", 
                            "Domain Body: ", head(domain.body.ord.df, n = 1)$term)
      }
      cluster.group <- c(cluster.group, rep(name.group, length(cluster.sentence)))
      
      for (name in name.sentence) {
        value.name <- c(value.name, name)
      }
      
      title.links <- c(title.links, get.links(unique.labels[index]))
      k <- k + 1
    }
  }
  index <- index + 1
}

# Data frame to show information
data.information <<- data.frame(cluster.group,
                                cluster.subgroup,
                                value.calls,
                                value.length,
                                value.name,
                                title.links)

data.information <<- data.information %>%
  rename(
    group = cluster.group,
    subgroup = cluster.subgroup,
    calls = value.calls,
    value = value.length,
    name = value.name,
    links = title.links
  )

# Construct hierarchical data
hierarchical.data <- data_to_hierarchical(data.information,
                                          group_vars = c("group", "subgroup"),
                                          size_var = "value")

# Serialising the results
saveRDS(list(hierarchical.data, data.information), file = file.results)