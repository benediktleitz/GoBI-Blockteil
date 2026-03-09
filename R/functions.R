library(tidyverse)

themeForSaving <- function() {
  theme_minimal() +
  theme(
    text = element_text(size = 14),
    axis.text.x = element_text(angle = 0, hjust = 1, size = 12),
    axis.text.y = element_text(size = 12),
    axis.title = element_text(size = 14),
    plot.title = element_text(size = 18, face = "bold"),
    plot.subtitle = element_text(size = 14),
    legend.position = c(0.98, 0.5),
    legend.justification = c(1, 1),
    legend.background = element_rect(fill = "white", colour = "grey80"),
    legend.text = element_text(size = 12),
    legend.title = element_text(size = 12)
  )
}

savePlotReport <- function(filepath, p){
  ggsave(filepath, p, width = 2100, height = 1200, units = "px")
}

sampleRandomGenes <- function(df, noZero, genesPerQuantile, nQuantile){
  
  if(noZero) df = df %>% filter(count > 0)
  
  df = df %>%
    mutate(quartile = ntile(count, nQuantile)) %>%   
    group_by(quartile) %>%
    slice_sample(n = genesPerQuantile) %>%                 
    ungroup()
  
  return(df)
}

parse_elapsed <- function(x) {
  parts <- strsplit(x, ":")
  sapply(parts, function(p) {
    mins <- as.numeric(p[1])
    secs <- as.numeric(p[2])
    mins * 60 + secs
  })
}

getMaxResSize <- function(verboseDF){
  return(as.numeric((verboseDF %>% filter(cmd == "Maximum resident set size (kbytes)") )$value))
}

plotMetric <- function(modeString, metric, func, 
                       save = F, 
                       df = long_summary, 
                       scaleFunc = scales::label_number(accuracy = 0.0001),
                       limitsArg = NULL){
  metricString <- deparse(substitute(metric))
  funcString <- deparse(substitute(func))
  p = df %>% 
    filter(mode == modeString) %>% 
    group_by(k, threshold) %>% 
    mutate(summarisedMetric = func({{ metric }})) %>%
    ggplot(aes(x = factor(k),
               y = factor(threshold),
               fill = summarisedMetric)) +
    geom_tile() +
    geom_text(
      aes(label = scaleFunc(summarisedMetric)),
      size = 2)  +
    scale_fill_gradient2(
      low = "#25ced1",
      high = "#ea526f",
      mid = "white",
      name =  metricString,
      limits = limitsArg,
      oob = scales::squish
    ) +
    coord_fixed() +
    labs(title = paste(modeString, metricString, funcString)) +
    theme_minimal() +
    themeForSaving() +
    theme(
      panel.grid = element_blank(),
      legend.position = "right",
      legend.justification = "center"
    )
  if(save){
    filename = paste(sep = "_", metricString, modeString, funcString)
    pathname = paste0(plotsDir, "HeatMaps/", filename, extType)
    ggsave(pathname, p)
  }
  return(p)
}
