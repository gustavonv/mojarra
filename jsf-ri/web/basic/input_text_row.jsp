           <tr>

             <td>

               <faces:output_text id="input_text1_label" 
                     value="input_text readonly"/>

             </td>


             <td>

               <faces:input_text id="input_text1" 
                                 value="Text Value 1" 
                                 readonly="true"
                                 size="12" maxlength="20"
                                 alt="input_text readonly"
                                 accesskey="D" 
                                 title="input_text readonly"/>


             </td>

            </tr>

           <tr>

             <td>

               <faces:output_text id="input_text2_label" 
                     value="input_text"/>

             </td>


             <td>

               <faces:input_text id="input_text2"
                                 value="Text Value 2" 
                                 alt="input_text"
                                  title="input_text"/>


             </td>

	      <td>

		<faces:output_errors id="input_text2_errors" 
			  compoundId="/standardRenderKitForm/input_text2" />

	      </td>

            </tr>

           <tr>

             <td>

               <faces:output_text id="input_text3_label" 
                     value="input_text"/>

             </td>


             <td>

               <faces:input_text id="input_text3"
                                 value="Text Value 3" 
                                 size="12"
                                 alt="input_text"
                                  title="input_text"/>


             </td>

	      <td>

		<faces:output_errors id="input_text3_errors" 
			  compoundId="/standardRenderKitForm/input_text3" />

	      </td>

            </tr>

           <tr>

             <td>

               <faces:output_text id="input_text4_label" 
                     value="input_text"/>

             </td>


             <td>

               <faces:input_text id="input_text4" 
                                 value="Text Value 4" 
                                 size="20" maxlength="40"
                                 alt="input_text"
                                 accesskey="d"
                               title="input_text"/>


             </td>

	      <td>

		<faces:output_errors id="input_text4_errors" 
			  compoundId="/standardRenderKitForm/input_text4" />

	      </td>

            </tr>

