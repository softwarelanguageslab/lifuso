generate.mds <- function(library.name, selected.feature.code, selected.feature.name) {
  print("Generating Markdown Files ...")
  old.wd <- getwd()
  md.folder <- "markdowns"
  setwd(md.folder)
  
  for (index.code in c(1:length(selected.feature.code))) {
    
    # Get feature name
    title.list <- selected.feature.name[index.code][[1]][[1]]
    body.list <- selected.feature.name[index.code][[1]][[2]]
    
    if (length(title.list) > 0) {
      data.title.df <- title.list[[1]]
      data.title.df.ord <- data.title.df[order(-data.title.df$freq),]

      action.title.df <- title.list[[2]]
      action.title.df.ord <- action.title.df[order(-action.title.df$freq),]

      data.title.top1<- head(data.title.df.ord, n=1)
      action.title.top1<- head(action.title.df.ord, n=1)
    }
    
    data.body.df <- body.list[[1]]
    data.body.df.ord <- data.body.df[order(-data.body.df$freq),]
    
    action.body.df <- body.list[[2]]
    action.body.df.ord <- action.body.df[order(-action.body.df$freq),]
    
    data.body.top5 <- head(data.body.df.ord, n=5)
    action.body.top5 <- head(action.body.df.ord, n=5)
    
    # All frequencies of the pairs
    top5s <- list(data.body.top5, action.body.top5)
    frequencies <- c(max(data.body.top5$freq), max(action.body.top5$freq))
    max.freq <- max(frequencies)
    index.max <- which(frequencies == max.freq)
    
    if (max.freq > 1) {
      valid.features <<- valid.features + 1
      
      if (length(index.max) > 1) {
        index.max <- head(index.max, n=1)
      }
      
      selected.top5 <- top5s[[index.max]]
      name.metadata <- ""
      
      if (index.max == 1) {
        unique.keys <- unique(selected.top5$term)
        
        if (length(unique.keys) == 1) {
          if (unique.keys[1] != "code") {
            associations <- selected.top5$assoc
            
            name.feature <- str_c("**", unique.keys[1], "**", "->", "(", paste(associations, collapse = ",") ,")")
            
            # Information about the metadata
            for (association in associations) {
              name.metadata <- str_c(name.metadata, association, " ", unique.keys[1], ",")
            }
          }
        } else {
          name.feature <- ""
          
          for (key in unique.keys) {
            if (key != "code") {
              key.assocs <- which(selected.top5$term == key)
              assocs <- selected.top5$assoc[key.assocs]
              
              if (length(assocs) == 1) {
                name.feature <- str_c(name.feature, "**", key, "**", "->", paste(assocs, collapse = ",") ," ")
                name.metadata <- str_c(name.metadata, assocs[1], " ", key, ",")
              } else {
                name.feature <- str_c(name.feature, "**", key, "**", "->", "(", paste(assocs, collapse = ",") ,") ")
                
                # Information about the metadata
                for (association in assocs) {
                  name.metadata <- str_c(name.metadata, association, " ", key, ",")
                }
              }
            }
          }
        }
      } else {
        unique.keys <- unique(selected.top5$assoc)
        
        if (length(unique.keys) == 1) {
          if (unique.keys[1] != "code") {
            associations <- selected.top5$term
            name.feature <- str_c("**", unique.keys[1], "**", "->", "(", paste(associations, collapse = ",") ,")")
            
            # Information about the metadata
            for (association in associations) {
              name.metadata <- str_c(name.metadata, association, " ", unique.keys[1], ",")
            }
          }
        } else {
          name.feature <- ""
          
          for (key in unique.keys) {
            if (unique.keys[1] != "code") {
              key.assocs <- which(selected.top5$assoc == key)
              assocs <- selected.top5$term[key.assocs]
              
              if (length(assocs) == 1) {
                name.feature <- str_c(name.feature, "**", key, "**", "->", paste(assocs, collapse = ",") ," ")
                name.metadata <- str_c(name.metadata, assocs[1], " ", key, ",")
              } else {
                name.feature <- str_c(name.feature, "**", key, "**", "->", "(", paste(assocs, collapse = ",") ,") ")
                
                # Information about the metadata
                for (association in assocs) {
                  name.metadata <- str_c(name.metadata, association, " ", key, ",")
                }
              }
            }
          }
        }
      }
      
      name.metadata <- substr(name.metadata, 1, nchar(name.metadata) - 1)
      
      # Get feature code
      code.description <- selected.feature.code[index.code][[1]]
      
      # Generating the data for the Markdown File
      data.str <- sprintf("#' ---\n#' title: Feature %s\n\n#' library: %s\n#' name: %s\n#' ---\n\n", valid.features, library.name, name.metadata)
      data.str <- str_c(data.str, sprintf("#' \n"))
      data.str <- str_c(data.str, sprintf("#' %s\n", name.feature))
      data.str <- str_c(data.str, sprintf("#' \n"))
      data.str <- str_c(data.str, sprintf("#' ##### API References\n"))
      data.str <- str_c(data.str, sprintf("#' \n"))
      data.str <- str_c(data.str, "#' ```java\n")
      
      calls.code <- strsplit(code.description, "<br>")[[1]]
      for (call in calls.code) {
        data.str <- str_c(data.str, sprintf("#' %s\n", call))
      }
      
      data.str <- str_c(data.str, "#' ```\n")
      script.name <- str_c(valid.features, ".R")
      
      file.connection <- file(script.name)
      writeLines(data.str, file.connection)
      close(file.connection)
      
      knitr::spin(script.name, format = "Rmd", knit = F)
      
      # Rename the Rmd file to md
      file.rename(sprintf("%s.Rmd", valid.features), sprintf("%s.md", valid.features))
      
      # Delete files with the extension .R
      unlink(sprintf("%s.R", valid.features))
    }
  }
  
  setwd(old.wd)
  print("Generation Done!")
}
