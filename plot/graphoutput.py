import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from scipy.signal import find_peaks

def fpeaks(x, y):
    #Find peaks
    peaks = find_peaks(y, height =100, threshold = 5, distance = 5)
    height = peaks[1]['peak_heights'] #list of the heights of the peaks
    peak_pos=[]
    for i in peaks[0]:
        peak_pos.append(x[i])
    #peak_pos = x[peaks] #list of the peaks positions


    #print("peak_pos",peak_pos)

    #Plotting
    return (peak_pos, height)


def slop(x,y):

    slopVector=[]
    x_axis=[]
    step=100

    for i in range (step, len(x)-step, step):
        slopVector.append((y[i+1] - y[i-step])/(x[i+1]-x[i-step]))
        x_axis.append(i)
        #   #print("slop ",i, " ", slop)
    vector=list(map(int, slopVector))
    print("vector ", vector)
    return (x_axis,vector)




def plot_graph_from_csv(file_paths):
    fig, ax = plt.subplots()

    for file_path in file_paths:
        data = pd.read_csv(file_path)
        x = data['x']
        y = data['y']
        z = data['z']
        ax.plot(x, y/10000, label=file_path)

        (x_axis, slopVector)=slop(x,y)

        ax.plot(x_axis[0:len(slopVector)], slopVector[0:len(x_axis)], label="line")

        (peak_pos, height)=fpeaks(x_axis,slopVector)
        print("pICOS SÃO :",peak_pos)
        ax.scatter(peak_pos, height, color = 'r', s = 15, marker = 'D', label = 'Maxima')
        # ax.scatter(min_pos, min_height*-1, color = 'gold', s = 15, marker = 'X', label = 'Minima')
        ax.legend()
        ax.grid()
        plt.show()

    ax.set_xlabel('X')
    ax.set_ylabel('Y')
    ax.set_title('Graph with Lines')
    ax.legend()
    plt.show()

# Example usage

file_paths = ['movies.csv']
#file_paths = ['cddb.csv', 'amazonGp.csv', "10K.csv", "dblpAcm.csv"]
plot_graph_from_csv(file_paths)