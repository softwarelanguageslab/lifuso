import numpy as np
import pandas as pd
from sklearn.neighbors import LocalOutlierFactor

def get_outliers(numbers, n_neighbours=2):
    if type(numbers) == int:
      return [numbers]
    
    n_neighbours = int(n_neighbours)
    number_pairs = list()
    
    for index, number in enumerate(numbers):
        number_pairs.append([index, number])

    df = pd.DataFrame(np.array(number_pairs), columns=["x", "y"])
    lof = LocalOutlierFactor(n_neighbors=n_neighbours)

    predictions = lof.fit_predict(df)
    outlier_indices = list()

    for index, outlier in enumerate(predictions):
        if outlier == -1:
            outlier_indices.append(index)
    
    return list(map(lambda index: numbers[index], outlier_indices))
