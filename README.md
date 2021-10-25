# SMART
Stroke Monitoring And Rehabilitation using Technology - A solution that aims to tackle the issues of expensive and inaccessible stroke care services in India

Stroke experts say it is crucial to develop innovative, technology-driven strategies to meet the growing needs of stroke survivors in India. SMART is a comprehensive solution for stroke care comprising of: 
 - SMART EEG headset
 - SMART app having diagnostic and rehabilitation tools.

I have developed the SMART EEG headset - a wearable EEG device costing less than ₹7,000 (US$100), several times cheaper than commercial EEG devices. It consists of:
 - An ergonomic 3-D printed headset, 
 - A signal processing module using a low-cost Arduino board and EEG Click (an IC to amplify and filter bio-signals). 

The EEG signals from the headset are analyzed using Fourier transforms (FFT) for classification into alpha, beta, and theta waves in the SMART app.

The SMART app developed using Android SDK in Java has two key modules:
 - Stroke Diagnostics
 - Stroke Rehabilitation

The diagnosis module uses EEG to calculate DAR levels and identify stroke patients and the rehabilitation module has an interactive game based on EEG neurofeedback training to help stroke patients improve their alpha brainwave activity and aid recovery. 

The diagnostic module also uses AI (Tensorflow models) to help detect early signs of stroke i.e. facial droop and arm weakness.

In order to test the reliability of the SMART EEG headset, I conducted experiments. Each experiment tested a particular stage of the EEG data acquisition process, namely: collecting the raw EEG data, calculating the frequency spectrum and lastly detecting changes in the brainwave state.

I believe SMART could radically improve the delivery of medical care to stroke patients as it is low-cost and easily-accessible, and it empowers the stroke patient to recover at home with minimal medical assistance. 

## Explainer video: <a name = "explainer-video"></a>

<p align = "center">
    <a href = "https://youtu.be/GeRhV_fclmc">
        <img src = "https://img.youtube.com/vi/GeRhV_fclmc/0.jpg">
    </a>
</p>
