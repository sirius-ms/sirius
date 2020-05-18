
package de.unijena.bioinf.babelms.cef;

import javax.xml.bind.annotation.*;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}p"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "p"
})
@XmlRootElement(name = "ChromPeaks")
public class ChromPeaks {

    @XmlElement(required = true)
    protected P p;

    /**
     * Gets the value of the p property.
     * 
     * @return
     *     possible object is
     *     {@link P }
     *     
     */
    public P getP() {
        return p;
    }

    /**
     * Sets the value of the p property.
     * 
     * @param value
     *     allowed object is
     *     {@link P }
     *     
     */
    public void setP(P value) {
        this.p = value;
    }

}
