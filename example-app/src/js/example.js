import { LocationTracker } from 'location--tracker';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    LocationTracker.echo({ value: inputValue })
}
