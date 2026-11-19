{{ instruction_p_what }}

### {{ instruction_h3_start }}

{{ placeholder_instruction_start_steps }}

### {{ instruction_h3_features }}

{{ placeholder_instruction_features }}

### {{ instruction_h3_script }}

{{ instruction_p_script }}

```js
let a = alarms.add({ time: '07:30', repeat: [1, 2, 3, 4, 5], label: '{{ instruction_example_alarm }}' });
console.log(a.repeatText, new Date(a.nextTrigger));
a.bind('./after-wake.js', { events: ['dismiss'] });

let t = alarms.timers.start('25m', { label: '{{ instruction_example_timer }}' });
console.log(t.id, new Date(t.endsAt).toLocaleTimeString());
```

{{ placeholder_instruction_script_notes }}

### {{ instruction_h3_requirements }}

{{ placeholder_instruction_requirements }}

### {{ instruction_h3_faq }}

{{ placeholder_instruction_faq }}

{{ instruction_p_more }}
