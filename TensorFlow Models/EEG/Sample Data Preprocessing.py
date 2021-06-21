file_input = open('D:\Downloads\IN.txt', 'r')
file_output = open('D:\Competitions\Stroke Project\Code\EEG\Sample EEG Data 3.txt', 'a')
for i in range(1000):
    x = file_input.readline()
    file_output.write('\n'.join((k.strip() for k in x.split(',')[6:])) + '\n')
