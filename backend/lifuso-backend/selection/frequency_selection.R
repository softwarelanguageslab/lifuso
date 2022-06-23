
frequency.selection <- function(results.calls, results.frequent) {
  methods.dataframe <- results.frequent$methods
  
  # Get the most frequent methods
  frequency.methods <- methods.dataframe$freq.methods
  unique.methods <- methods.dataframe$unique.methods
  precedents.methods <- methods.dataframe$precedents
  
  if (length(frequency.methods) == 1) {
    return(list(c(str_c(precedents.methods[1], ".", unique.methods[1]))))
  }
  
  outliers <- get_outliers(frequency.methods)
  
  if (length(outliers) == 0) {
    outliers <- get_outliers(frequency.methods, 1)
  }
  
  if (length(outliers) == 0) {
    return(list(c()))
  }
  
  last.outlier <- outliers[length(outliers)]
  data.iterate <- which(frequency.methods == last.outlier)
  selected.calls <- c()
  
  # Generate the calls from the methods' precedence
  for (index.methods in c(1:data.iterate)) {
    selected.precedents.divided <- strsplit(precedents.methods[index.methods], ",")[[1]]
    calls.generated <- unname(unlist(Map(function(precedent) str_c(precedent, ".", unique.methods[index.methods]), 
                                         selected.precedents.divided)))
    
    selected.calls <- c(selected.calls, calls.generated)
  }
  
  return(list(selected.calls))
}

# Function that extracts individual patterns
process.name <- function(name, pattern) {
  name.divided <- strsplit(name, "\\|")[[1]]
  pattern.word <- c()
  
  if (length(name.divided) >= 4) {
    index <- 1
    for (word in name.divided) {
      if (startsWith(word, pattern)) {
        pattern.word <- c(pattern.word, name.divided[index - 1])
      }
      index <- index + 1
    }
  }
  return(pattern.word)
}

# Obtain the frequent terms with LOF
frequent.name.terms <- function(all.names) {
  unique.names <- unique(all.names)
  frequency.elements <- unlist(lapply(unique.names, function(element) length(which(all.names == element))))
  sorted.frequencies <- sort(frequency.elements, decreasing = T)
  outliers.names <- get_outliers(sorted.frequencies)
  
  if (length(outliers.names) == 0) {
    outliers.names <- get_outliers(sorted.frequencies, 1)
  }
  
  if (length(outliers.names) == 0) {
    outliers.names <- max(sorted.frequencies)
  }
  
  last.outlier <- outliers.names[length(outliers.names)]
  data.iterate <- which(sorted.frequencies == last.outlier)
  
  obtain.selected.elements <- function(data.index) {
    index.name <- sorted.frequencies[data.index]
    higher.frequencies <- which(frequency.elements >= index.name)
    
    selected.elements <- unique.names[higher.frequencies]
    selected.elements <- Filter(function(name) !grepl("<", name), selected.elements)
    selected.elements <- Filter(function(name) !grepl(">", name), selected.elements)
    
    return(selected.elements)
  }
  
  return(obtain.selected.elements(data.iterate))
}

# Function given a term, extracts their associated counterpart
obtain.associations <- function(all.pairs, pattern, term) {
  term.words <- c()
  assocs <- c()
  
  counter.pattern <- "VB"
  
  if (pattern == "VB")
    counter.pattern <- "NN"
  
  for (name in all.pairs) {
    name.divided <- strsplit(name, "\\|")[[1]]
    
    if (length(name.divided) >= 4) {
      if (term %in% name.divided) {
        index <- 1
        flag <- F
        temp.assocs <- c()
        
        for (word in name.divided) {
          if (word == term) {
            if (startsWith(name.divided[index + 1], pattern)) {
              flag <- T
            }
          } else {
            if (index + 1 <= length(name.divided) && startsWith(name.divided[index + 1], counter.pattern)) {
              temp.assocs <- c(temp.assocs, word)
            }
          }
          index <- index + 1
        }
        
        if (flag) {
          assocs <- c(assocs, temp.assocs)
        }
      }
    }
  }
  
  unique.assocs <- unique(assocs)
  freq.assocs <- unlist(lapply(unique.assocs, function(assoc) length(which(assocs == assoc))))
  term.words <- rep(term, length(unique.assocs))
  
  return(data.frame(term=term.words, assoc=unique.assocs, freq=freq.assocs))
}

# Compute the relations extracted from the pairs in the specified text
compute.relations <- function(pairs, verbs, nouns) {
  selected.verbs <- frequent.name.terms(verbs)
  relations.verb <- data.frame()
  
  for (verb in selected.verbs) {
    associations.verb <- obtain.associations(pairs, "VB", verb)
    relations.verb <- rbind(relations.verb, associations.verb)
  }
  
  selected.nouns <- frequent.name.terms(nouns)
  relations.noun <- data.frame()
  
  for (noun in selected.nouns) {
    associations.noun <- obtain.associations(pairs, "NN", noun)
    relations.noun <- rbind(relations.noun, associations.noun)
  }
  
  return(list(relations.noun, relations.verb))
}

classify.terms <- function(term) {
  stopwords <- readLines("selection/stopwords.txt")
  return(nchar(term) == 1 || grepl("#", term, fixed = T) || term %in% stopwords)
}

cluster.name.titles <- function(all.names) {
  title.pairs <- Filter(function(statement) grepl("_T", statement, fixed = T), all.names)
  
  title.verbs <- unlist(lapply(title.pairs, function(pair) process.name(pair, "VB")))
  title.verbs <- Filter(function(term) !classify.terms(term), title.verbs)
  
  title.nouns <- unlist(lapply(title.pairs, function(pair) process.name(pair, "NN")))
  title.nouns <- Filter(function(term) !classify.terms(term), title.nouns)
  
  body.pairs <- Filter(function(statement) !grepl("_T", statement, fixed = T), all.names)
  
  body.verbs <- unlist(lapply(body.pairs, function(pair) process.name(pair, "VB")))
  body.verbs <- Filter(function(term) !classify.terms(term), body.verbs)
  
  body.nouns <- unlist(lapply(body.pairs, function(pair) process.name(pair, "NN")))
  body.nouns <- Filter(function(term) !classify.terms(term), body.nouns)
  
  if (length(title.pairs) == 0 || length(title.verbs) == 0) {
    return(list(list(),
                compute.relations(body.pairs, body.verbs, body.nouns)))
  }
  
  return(list(compute.relations(title.pairs, title.verbs, title.nouns),
              compute.relations(body.pairs, body.verbs, body.nouns)))
}

highlight.apis <- function(selected.apis, all.usages) {
  usages.highlighted <- c()
  
  index.snippet <- 1
  for (usage in all.usages) {
    usage.divided <- strsplit(usage, "<br>")[[1]]
    usage.highlighted <- c()
    
    for (call in usage.divided) {
      if (call %in% selected.apis) {
        call.highlighted <- str_c('<p style="color:red;">', call, "</p>")
        usage.highlighted <- c(usage.highlighted, call.highlighted)
      } else {
        call.highlighted <- str_c('<p style="color:blue;">', call, "</p>")
        usage.highlighted <- c(usage.highlighted, call.highlighted)
      }
    }
    
    usage.replaced <- paste(usage.highlighted, collapse="<br>")
    usages.highlighted <- c(usages.highlighted, str_c("<b>Snippet-", index.snippet, "</b><br>", usage.replaced))

    index.snippet <- index.snippet + 1
  }
  
  return(usages.highlighted)
}

make.apis.df <- function(filtered.data_calls) {
  # Frequency of API calls
  all.api_calls <- c()
  
  for (data.call in filtered.data_calls) {
    divided.calls <- strsplit(data.call, "<br>")[[1]]
    all.api_calls <- c(all.api_calls, divided.calls)
  }
  
  unique.api_calls <- unique(all.api_calls)
  freq.api_calls <- unlist(lapply(unique.api_calls, function(api.call) length(which(api.call == all.api_calls))))
  percent.api_calls <- freq.api_calls / length(filtered.data_calls) * 100
  percent.api_calls <- unlist(lapply(percent.api_calls, function(freq) if (freq > 100) 100 else freq))
  
  data.api_calls <- data.frame(unique.api_calls, freq.api_calls, percent.api_calls)
  data.api_calls.ord <- data.api_calls[order(-data.api_calls$percent.api_calls),]
  
  return(data.api_calls.ord)
}


frequent.apis <- function(frequency.methods, data.api_calls.ord, percent.information = T) {
  # Most frequent APIs
  outliers.methods <- get_outliers(frequency.methods)
  
  if (length(outliers.methods) == 0) {
    outliers.methods <- get_outliers(frequency.methods, 1)
  }
  
  number.outlier.methods <- length(outliers.methods)
  last.outlier <- outliers.methods[number.outlier.methods]
  data.iterate <- which(frequency.methods == last.outlier)
  
  if (length(data.iterate) > 1) {
    data.iterate <- data.iterate[length(data.iterate)]
  }
  
  selected.apis.df <- head(data.api_calls.ord, n = data.iterate)
  
  last.percentage <- tail(selected.apis.df$percent.api_calls, n = 1)
  new.data.iterate <- length(which(data.api_calls.ord$percent.api_calls >= last.percentage))
  selected.apis.df <- head(data.api_calls.ord, n = new.data.iterate)
  
  calls.feature <- selected.apis.df$unique.api_calls
  percent.feature <- selected.apis.df$percent.api_calls
  
  if (percent.information) {
    calls.percent <- c()
    
    k <- 1
    for (call in calls.feature) {
      percent <- percent.feature[k]
      calls.percent <- c(calls.percent, str_c(call, " --> ", round(percent, 2), " %"))
      
      k <- k + 1
    }
    
    return(code.feature <- paste0(calls.percent, collapse = "<br>"))
  } else {
    return(calls.feature)
  }
}
