import matplotlib.pyplot as plt
import pandas as pd

def plot_graph_from_csv(file_paths):
    fig, ax = plt.subplots()

    for file_path in file_paths:
        data = pd.read_csv(file_path)
        x = data['x']
        y = data['y']
        z = data['z']
        ax.plot(x[1:30000], y[1:30000], label=file_path)

    ax.set_xlabel('X')
    ax.set_ylabel('Y')
    ax.set_title('Graph with Lines')
    ax.legend()
    plt.show()

# Example usage

file_paths = ['cddb.csv', 'amazonGp.csv', "10K.csv", "dblpAcm.csv","movies.csv", "dblpScholar.csv"]

plot_graph_from_csv(file_paths)