export class dynamicLoader {
    constructor(parentElement, template, scripts) {
        this.scriptElements = [];
        this.scripts = scripts;
        this.exportedHTML = template;
        this.template;
        this.parent = parentElement;
        this.appendElement();
    }

    // Template
    getTemplate() {
        return this.exportedHTML;
    }

    // Handle script load : 
    loadScript(url) {
        return new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = url;
            script.defer = false;
            script.type = 'module';
            script.async = true;
            script.onload = resolve;
            script.onerror = reject;
            document.head.appendChild(script);
        });
    }

    // Append element and load script
    appendElement() {
        this.template = document.createElement('section');
        this.template.innerHTML = this.getTemplate();
        this.template.style.display = 'block';
        this.parent.appendChild(this.template);

        // Loads scripts
        this.scripts.forEach((element) => {
            this.loadScript(element).then((scriptElement) => {
                this.scriptElements.push(scriptElement); // Store the script element reference
            }).catch((error) => {
                console.error(element + ' has not been loaded correctly', error);
            });
        });
    }

    show() { //Show section
        if (this.template) {
            this.template.style.display = 'block';
        }
    }

    hide() { //Hide section
        if (this.template) {
            this.template.style.display = 'none';
        }
    }
}