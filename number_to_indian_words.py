def number_to_indian_words(number):
    units = ["", "Thousand", "Lakh", "Crore"]
    words = []

    if number == 0:
        return "Zero"

    num = int(number)
    parts = []

    # Break number into parts according to Indian system
    parts.append(num % 1000)       # units
    num //= 1000
    parts.append(num % 100)        # thousands
    num //= 100
    parts.append(num % 100)        # lakhs
    num //= 100
    parts.append(num)              # crores

    units = units[:len(parts)]

    for i in range(len(parts)):
        if parts[i]:
            words.append(f"{parts[i]} {units[i]}")

    return ' '.join(reversed(words)).strip()
