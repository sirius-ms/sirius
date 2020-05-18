
package de.unijena.bioinf.babelms.cef;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


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
 *         &lt;element ref="{}CalStep" maxOccurs="unbounded"/>
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
    "calStep"
})
@XmlRootElement(name = "MassCalibration")
public class MassCalibration {

    @XmlElement(name = "CalStep", required = true)
    protected List<CalStep> calStep;

    /**
     * Gets the value of the calStep property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the calStep property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCalStep().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CalStep }
     * 
     * 
     */
    public List<CalStep> getCalStep() {
        if (calStep == null) {
            calStep = new ArrayList<CalStep>();
        }
        return this.calStep;
    }

}
